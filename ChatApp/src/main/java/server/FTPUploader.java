package server;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.FileInputStream;
import java.io.IOException;

public class FTPUploader {

    private final String server;
    private final int port;
    private final String user;
    private final String pass;


    public FTPUploader(String server, int port, String user, String pass) {
        this.server = server;
        this.port = port;
        this.user = user;
        this.pass = pass;
    }

    public boolean uploadFile(String localFilePath, String remoteDir, String remoteFileName) {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            boolean login = ftpClient.login(user, pass);

            if (!login) {
                System.out.println("❌ FTP login failed");
                return false;
            }

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            if (remoteDir != null && !remoteDir.isEmpty()) {
                ftpClient.makeDirectory(remoteDir);
                ftpClient.changeWorkingDirectory(remoteDir);
            }

            try (FileInputStream inputStream = new FileInputStream(localFilePath)) {
                boolean done = ftpClient.storeFile(remoteFileName, inputStream);
                if (done) {
                    System.out.println("✅ File uploaded to FTP: " + remoteFileName);
                    return true;
                } else {
                    System.out.println("❌ FTP upload failed");
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
