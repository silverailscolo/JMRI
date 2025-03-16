package jmri.jmrix.nce.consist;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.text.MessageFormat;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
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
 * Backups NCE Consists to a text file format defined by NCE.
 * <p>
 * NCE "Backup consists" dumps the consists into a text file. The consists data
 * are stored in the NCE CS starting at xF500 and ending at xFAFF.
 * <p>
 * NCE file format:
 * <p>
 * :F500 (16 bytes per line, grouped as 8 words with space delimiters) :F510 . .
 * :FAF0 :0000
 * <p>
 * Consist data byte:
 * <p>
 * bit 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1 0
 * <p>
 * This backup routine uses the same consist data format as NCE.
 *
 * @author Dan Boudreau Copyright (C) 2007
 * @author Ken Cameron Copyright (C) 2023
 */
public class NceConsistBackup extends Thread implements jmri.jmrix.nce.NceListener {

    private int consistRecLen = 16; // bytes per line
    private int replyLen = 0; // expected byte length
    private int waiting = 0; // to catch responses not intended for this module
    private boolean fileValid = false; // used to flag backup status messages

    private final byte[] nceConsistData = new byte[consistRecLen];

    JLabel textConsist = new JLabel();
    JLabel consistNumber = new JLabel();

    private NceTrafficController tc = null;
    private int consistStartNum = -1;
    private int consistEndNum = -1;

    public NceConsistBackup(NceTrafficController t) {
        tc = t;
        consistStartNum = tc.csm.getConsistMin();
        consistEndNum = tc.csm.getConsistMax();
    }

    @Override
    public void run() {

        // get file to write to
        JFileChooser fc = new jmri.util.swing.JmriJFileChooser(FileUtil.getUserFilesPath());
        fc.addChoosableFileFilter(new TextFilter());

        File fs = new File("NCE consist backup.txt"); // NOI18N
        fc.setSelectedFile(fs);

        int retVal = fc.showSaveDialog(null);
        if (retVal != JFileChooser.APPROVE_OPTION) {
            return; // Canceled
        }
        if (fc.getSelectedFile() == null) {
            return; // Canceled
        }
        File f = fc.getSelectedFile();
        if (fc.getFileFilter() != fc.getAcceptAllFileFilter()) {
            // append .txt to file name if needed
            String fileName = f.getAbsolutePath();
            String fileNameLC = fileName.toLowerCase();
            if (!fileNameLC.endsWith(".txt")) {
                fileName = fileName + ".txt";
                f = new File(fileName);
            }
        }
        if (f.exists()) {
            if (JmriJOptionPane.showConfirmDialog(null,
                    MessageFormat.format(Bundle.getMessage("FileExists"), f.getName()),
                    Bundle.getMessage("OverwriteFile"),
                    JmriJOptionPane.OK_CANCEL_OPTION) != JmriJOptionPane.OK_OPTION) {
                return;
            }
        }

        try (PrintWriter fileOut = new PrintWriter(new BufferedWriter(new FileWriter(f)),
                true)) {

            if (JmriJOptionPane.showConfirmDialog(null,
                    Bundle.getMessage("BackupTakesAwhile"),
                    Bundle.getMessage("NceConsistBackup"),
                    JmriJOptionPane.YES_NO_OPTION) != JmriJOptionPane.YES_OPTION) {
                fileOut.close();
                return;
            }

            // create a status frame
            JPanel ps = new JPanel();
            jmri.util.JmriJFrame fstatus = new jmri.util.JmriJFrame(Bundle.getMessage("NceConsistBackup"));
            fstatus.setLocationRelativeTo(null);
            fstatus.setSize(300, 100);
            fstatus.getContentPane().add(ps);

            ps.add(textConsist);
            ps.add(consistNumber);

            textConsist.setText(Bundle.getMessage("ConsistLineNumber"));
            textConsist.setVisible(true);
            consistNumber.setVisible(true);

            // now read NCE CS consist memory and write to file
            waiting = 0; // reset in case there was a previous error
            fileValid = true; // assume we're going to succeed
            // output string to file
            // head 2 bytes, end 2 bytes, mid num * 2 bytes
            int consistBytesEach = (tc.csm.getConsistMidEntries() * 2) + 2 + 2;
            int memOffsetEnd = consistBytesEach * (1 +consistEndNum - consistStartNum);
            int consistRecNum = 0;
            int consistNum = consistStartNum;

            for (int memOffset = 0; memOffset < memOffsetEnd; memOffset += consistRecLen) {
                
                consistNum = consistStartNum + (memOffset / consistBytesEach);
                consistRecNum = memOffset / consistRecLen;
                consistNumber.setText(Integer.toString(consistNum));
                fstatus.setVisible(true);

                getNceConsist(consistRecNum);

                if (!fileValid) {
                    memOffset = memOffsetEnd; // break out of for loop
                }
                if (fileValid) {
                    StringBuilder buf = new StringBuilder();
                    buf.append(":").append(Integer.toHexString(tc.csm.getConsistHeadAddr() + memOffset));

                    for (int i = 0; i < consistRecLen; i++) {
                        buf.append(" ").append(StringUtil.twoHexFromInt(nceConsistData[i++]));
                        buf.append(StringUtil.twoHexFromInt(nceConsistData[i]));
                    }

                    log.debug("consist {}", buf);
                    fileOut.println(buf.toString());
                }
            }

            if (fileValid) {
                // NCE file terminator
                String line = ":0000";
                fileOut.println(line);
            }

            // Write to disk and close file
            fileOut.flush();
            fileOut.close();

            // kill status panel
            fstatus.dispose();

            if (fileValid) {
                JmriJOptionPane.showMessageDialog(null,
                        Bundle.getMessage("SuccessfulBackup"),
                        Bundle.getMessage("NceConsistBackup"),
                        JmriJOptionPane.INFORMATION_MESSAGE);
            } else {
                JmriJOptionPane.showMessageDialog(null,
                        Bundle.getMessage("BackupFailed"),
                        Bundle.getMessage("NceConsistBackup"),
                        JmriJOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException e) {
            // this is the end of the try-with-resources that opens fileOut.
        }

    }

    // Read 16 bytes of NCE CS memory
    private void getNceConsist(int cN) {

        NceMessage m = readConsistMemory(cN);
        tc.sendNceMessage(m, this);
        // wait for read to complete
        readWait();
    }

    // wait up to 30 sec per read
    private boolean readWait() {
        int waitcount = 30;
        while (waiting > 0) {
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // retain if needed later
                }
            }
            if (waitcount-- < 0) {
                log.error("read timeout"); // NOI18N
                fileValid = false; // need to quit
                return false;
            }
        }
        return true;
    }

    /*
     * // USB set cab memory pointer private void setUsbCabMemoryPointer(int
     * cab, int offset) { log.debug("Macro base address: {}, offset: {}",
     * Integer.toHexString(cab), offset); replyLen = NceMessage.REPLY_1; //
     * Expect 1 byte response waiting++; byte[] bl =
     * NceBinaryCommand.usbMemoryPointer(cab, offset); NceMessage m =
     * NceMessage.createBinaryMessage(tc, bl, NceMessage.REPLY_1);
     * tc.sendNceMessage(m, this); }
     * 
     * // USB Read N bytes of NCE cab memory private void readUsbMemoryN(int
     * num) { switch (num) { case 1: replyLen = NceMessage.REPLY_1; // Expect 1
     * byte response break; case 2: replyLen = NceMessage.REPLY_2; // Expect 2
     * byte response break; case 4: replyLen = NceMessage.REPLY_4; // Expect 4
     * byte response break; default: log.error("Invalid usb read byte count");
     * return; } waiting++; byte[] bl = NceBinaryCommand.usbMemoryRead((byte)
     * num); NceMessage m = NceMessage.createBinaryMessage(tc, bl, replyLen);
     * tc.sendNceMessage(m, this); }
     * 
     * // USB Write 1 byte of NCE memory private void writeUsbMemory1(byte
     * value) { log.debug("Write byte: {}", String.format("%2X", value));
     * replyLen = NceMessage.REPLY_1; // Expect 1 byte response waiting++;
     * byte[] bl = NceBinaryCommand.usbMemoryWrite1(value); NceMessage m =
     * NceMessage.createBinaryMessage(tc, bl, NceMessage.REPLY_1);
     * tc.sendNceMessage(m, this); }
     */
    
    // Reads 16 bytes of NCE consist memory
    private NceMessage readConsistMemory(int consistNum) {

        int nceConsistAddr = (consistNum * consistRecLen) + tc.csm.getConsistHeadAddr();
        replyLen = NceMessage.REPLY_16; // Expect 16 byte response
        waiting++;
        byte[] bl = NceBinaryCommand.accMemoryRead(nceConsistAddr);
        NceMessage m = NceMessage.createBinaryMessage(tc, bl, NceMessage.REPLY_16);
        return m;
    }

    @Override
    public void message(NceMessage m) {
    } // ignore replies

    @SuppressFBWarnings(value = "NN_NAKED_NOTIFY")
    // this reply always expects two consecutive reads
    @Override
    public void reply(NceReply r) {

        if (waiting <= 0) {
            log.error("unexpected response"); // NOI18N
            return;
        }
        if (r.getNumDataElements() != replyLen) {
            log.error("reply length incorrect"); // NOI18N
            return;
        }

        // load data buffer
        for (int i = 0; i < NceMessage.REPLY_16; i++) {
            nceConsistData[i] = (byte) r.getElement(i);
        }
        waiting--;

        // wake up backup thread
        synchronized (this) {
            notify();
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NceConsistBackup.class);

}
