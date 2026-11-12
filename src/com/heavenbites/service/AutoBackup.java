package com.heavenbites.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Background thread that copies the data files into data/backup every 5 minutes. */
public class AutoBackup extends Thread {

    public AutoBackup() {
        setDaemon(true);
        setName("auto-backup");
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(5 * 60 * 1000);
                File dir = new File(DataStore.DIR);
                File bk = new File(dir, "backup");
                bk.mkdirs();
                File[] files = dir.listFiles((d, n) -> n.endsWith(".txt"));
                if (files != null) {
                    synchronized (com.heavenbites.util.FileUtil.class) {
                        for (File f : files) {
                            Files.copy(f.toPath(), new File(bk, f.getName()).toPath(),
                                    StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            } catch (InterruptedException e) {
                return;
            } catch (IOException e) {
                System.out.println("Backup failed: " + e.getMessage());
            }
        }
    }
}