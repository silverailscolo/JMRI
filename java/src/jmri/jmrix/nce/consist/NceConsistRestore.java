package jmri.jmrix.nce.consist;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import jmri.jmrix.nce.NceBinaryCommand;
import jmri.jmrix.nce.NceMessage;
import jmri.jmrix.nce.NceReply;
import jmri.jmrix.nce.NceTrafficController;
import jmri.util.FileUtil;
import jmri.util.StringUtil;
import jmri.util.swing.JmriJOptionPane;
import jmri.util.swing.TextFilter;

/**
 * Restores NCE consists from a text file defined by NCE.
 * <p>
 * NCE file format:
 * <p>
 * :F500 (16 bytes per line, grouped as 8 words with space delimiters) :F510 . .
 * :FAF0 :0000
 * <p>
 * The restore routine checks that each line of the file begins with the
 * appropriate consist address.
 *
 * @author Dan Boudreau Copyright (C) 2007
 * @author Ken Cameron Copyright (C) 2023
 */
public class NceConsistRestore extends Thread implements jmri.jmrix.nce.NceListener {

    private static final int CONSIST_LNTH = 16; // 16 bytes per consist line
    private static final int REPLY_1 = 1; // reply length of 1 byte expected
    private int replyLen = 0; // expected byte length
    private int waiting = 0; // to catch responses not intended for this module
    private boolean fileValid = false; // used to flag status messages

    javax.swing.JLabel textConsist = new javax.swing.JLabel();
    javax.swing.JLabel consistNumber = new javax.swing.JLabel();

    private NceTrafficController tc = null;

    public NceConsistRestore(NceTrafficController t) {
        super();
        this.tc = t;
    }

    @Override
    public void run() {

        // Get file to read from
        JFileChooser fc = new jmri.util.swing.JmriJFileChooser(FileUtil.getUserFilesPath());
        fc.addChoosableFileFilter(new TextFilter());
        int retVal = fc.showOpenDialog(null);
        if (retVal != JFileChooser.APPROVE_OPTION) {
            return; // Canceled
        }
        if (fc.getSelectedFile() == null) {
            return; // Canceled
        }
        File f = fc.getSelectedFile();
        try (BufferedReader in = new BufferedReader(new FileReader(f))) {
            // create a status frame
            JPanel ps = new JPanel();
            jmri.util.JmriJFrame fstatus = new jmri.util.JmriJFrame(Bundle.getMessage("NceConsistRestore"));
            fstatus.setLocationRelativeTo(null);
            fstatus.setSize(300, 100);
            fstatus.getContentPane().add(ps);

            ps.add(textConsist);
            ps.add(consistNumber);

            textConsist.setText(Bundle.getMessage("ConsistLineNumber"));
            textConsist.setVisible(true);
            consistNumber.setVisible(true);

            // Now read the file and check the consist address
            waiting = 0;
            fileValid = false; // in case we break out early
            int consistNum = 0; // for user status messages
            int curConsist = tc.csm.getConsistHeadAddr(); // load the start address of the NCE consist memory
            byte[] consistData = new byte[CONSIST_LNTH]; // NCE Consist data
            String line;
            int consistMemMim = tc.csm.getConsistHeadAddr();
            int consistMemMax = consistMemMim + 0x100 + 0x100 + tc.csm.getConsistMidSize();
            int consistMemAddr;

            while (true) {
                line = in.readLine();

                consistNumber.setText(Integer.toString(consistNum++));

                if (line == null) { // while loop does not break out quick enough
                    log.error("NCE consist file terminator :0000 not found"); // NOI18N
                    break;
                }
                
                log.debug("consist {}", line);
                
                // check that each line contains the NCE memory address of the consist
                String consistAddr = ":" + Integer.toHexString(curConsist);
                String[] consistLine = line.split(" ");

                // check for end of consist terminator
                if (consistLine[0].equalsIgnoreCase(":0000")) {
                    fileValid = true; // success!
                    break;
                }
                
                // check for consistLine our of range
                consistMemAddr = Integer.parseUnsignedInt(consistLine[0].replace(":", ""), 16);
                if (consistMemAddr < consistMemMim) {
                    log.warn("consist mem file out of range, ending restore, got: {} mimimum: {} ",
                            Integer.toHexString(consistMemAddr), Integer.toHexString(consistMemMim));
                    fileValid = true;
                    break;
                }
                if (consistMemAddr >= consistMemMax ) {
                    log.warn("consist mem file out of range, ending restore, got: {} maximum: {} ",
                            Integer.toHexString(consistMemAddr), Integer.toHexString(consistMemMax));
                    fileValid = true;
                    break;
                }
                if (consistMemAddr != curConsist) {
                    log.warn("consist mem file out of range, ending restore, got: {} expected: {} ",
                            Integer.toHexString(consistMemAddr), Integer.toHexString(curConsist));
                    fileValid = true;
                    break;
                }

                if (!consistAddr.equalsIgnoreCase(consistLine[0])) {
                    log.error("Restore file selected is not a valid backup file"); // NOI18N
                    log.error("Consist memory address in restore file should be {}, read {}",
                            consistAddr, consistLine[0]); // NOI18N
                    break;
                }

                // consist file found, give the user the choice to continue
                if (curConsist == tc.csm.getConsistHeadAddr()) {
                    if (JmriJOptionPane.showConfirmDialog(null,
                            Bundle.getMessage("RestoreTakesAwhile"),
                            Bundle.getMessage("NceConsistRestore"),
                            JmriJOptionPane.YES_NO_OPTION) != JmriJOptionPane.YES_OPTION) {
                        break;
                    }
                }

                fstatus.setVisible(true);

                // now read the entire line from the file and create NCE message
                for (int i = 0; i < 8; i++) {
                    int j = i << 1; // i = word index, j = byte index

                    byte b[] = StringUtil.bytesFromHexString(consistLine[i + 1]);

                    consistData[j] = b[0];
                    consistData[j + 1] = b[1];
                }

                NceMessage m = writeNceConsistMemory(curConsist, consistData);
                tc.sendNceMessage(m, this);

                curConsist += CONSIST_LNTH;

                // wait for write to NCE CS to complete
                if (waiting > 0) {
                    synchronized (this) {
                        try {
                            wait(20000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // retain if needed later
                        }
                    }
                }
                // failed
                if (waiting > 0) {
                    log.error("timeout waiting for reply"); // NOI18N
                    break;
                }
            }
            in.close();

            // kill status panel
            fstatus.dispose();

            if (fileValid) {
                JmriJOptionPane.showMessageDialog(null,
                        Bundle.getMessage("SuccessfulRestore"),
                        Bundle.getMessage("NceConsistRestore"),
                        JmriJOptionPane.INFORMATION_MESSAGE);
            } else {
                JmriJOptionPane.showMessageDialog(null,
                        Bundle.getMessage("RestoreFailed"),
                        Bundle.getMessage("NceConsistRestore"),
                        JmriJOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            // this is the end of the try-with-resources that opens in.
        }
    }
//
//    // USB set cab memory pointer
//    private void setUsbCabMemoryPointer(int cab, int offset) {
//        log.debug("Macro base address: {}, offset: {}", Integer.toHexString(cab), offset);
//        replyLen = NceMessage.REPLY_1; // Expect 1 byte response
//        waiting++;
//        byte[] bl = NceBinaryCommand.usbMemoryPointer(cab, offset);
//        NceMessage m = NceMessage.createBinaryMessage(tc, bl, NceMessage.REPLY_1);
//        tc.sendNceMessage(m, this);
//    }
//
//    // USB Read N bytes of NCE cab memory
//    private void readUsbMemoryN(int num) {
//        switch (num) {
//            case 1:
//                replyLen = NceMessage.REPLY_1; // Expect 1 byte response
//                break;
//            case 2:
//                replyLen = NceMessage.REPLY_2; // Expect 2 byte response
//                break;
//            case 4:
//                replyLen = NceMessage.REPLY_4; // Expect 4 byte response
//                break;
//            default:
//                log.error("Invalid usb read byte count");
//                return;
//        }
//        waiting++;
//        byte[] bl = NceBinaryCommand.usbMemoryRead((byte) num);
//        NceMessage m = NceMessage.createBinaryMessage(tc, bl, replyLen);
//        tc.sendNceMessage(m, this);
//    }
//
//    // USB Write 1 byte of NCE memory
//    private void writeUsbMemory1(byte value) {
//        log.debug("Write byte: {}", String.format("%2X", value));
//        replyLen = NceMessage.REPLY_1; // Expect 1 byte response
//        waiting++;
//        byte[] bl = NceBinaryCommand.usbMemoryWrite1(value);
//        NceMessage m = NceMessage.createBinaryMessage(tc, bl, NceMessage.REPLY_1);
//        tc.sendNceMessage(m, this);
//    }

    // writes 16 bytes of NCE consist memory
    private NceMessage writeNceConsistMemory(int curConsist, byte[] b) {

        replyLen = REPLY_1; // Expect 1 byte response
        waiting++;

        byte[] bl = NceBinaryCommand.accMemoryWriteN(curConsist, b);
        NceMessage m = NceMessage.createBinaryMessage(tc, bl, REPLY_1);
        return m;
    }

    @Override
    public void message(NceMessage m) {
    } // ignore replies

    @SuppressFBWarnings(value = "NN_NAKED_NOTIFY")
    @Override
    public void reply(NceReply r) {
        log.debug("waiting for {} responses", waiting); // NOI18N

        if (waiting <= 0) {
            log.error("unexpected response"); // NOI18N
            return;
        }
        waiting--;
        if (r.getNumDataElements() != replyLen) {
            log.error("reply length incorrect"); // NOI18N
            return;
        }
        if (replyLen == REPLY_1) {
            // Looking for proper response
            if (r.getElement(0) != NceMessage.NCE_OKAY) {
                log.error("reply incorrect"); // NOI18N
            }
        }

        // wake up restore thread
        if (waiting == 0) {
            synchronized (this) {
                notify();
            }
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NceConsistRestore.class);

}
