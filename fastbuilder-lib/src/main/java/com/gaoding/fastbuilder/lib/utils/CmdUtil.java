package com.gaoding.fastbuilder.lib.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by WYJ on 2016-08-17.
 */
public class CmdUtil {

    public static List<String> cmd2(String cmd) {
        return cmd2(cmd, null);
    }

    public static List<String> cmd2(String cmd, String pathName) {
        List<String> result = new ArrayList<>();
        Log.i(cmd);
        BufferedReader br = null;
        try {
            Process p;
            if (pathName == null) {
                p = Runtime.getRuntime().exec(cmd);
            } else {
                p = Runtime.getRuntime().exec(cmd, null, new File(pathName));
            }
            br = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("GBK")));
            String line = null;
            while ((line = br.readLine()) != null) {
                result.add(line);
            }

            br = new BufferedReader(new InputStreamReader(p.getErrorStream(), Charset.forName("GBK")));
            while ((line = br.readLine()) != null) {
                Log.i(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    public static boolean cmd(List<String> list) {
        return cmd(getCmd(list));
    }

    public static boolean cmd(String cmd) {
        Log.i(cmd);
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            streamForwarder(p.getErrorStream(), true);
            streamForwarder(p.getInputStream(), false);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    public static String getCmd(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String value : list) {
            sb.append(value).append(" ");
        }
        return sb.toString();
    }

    public static void streamForwarder(InputStream is, boolean isError) {
        try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (isError) {
                    Log.e(line);
                } else {
                    Log.i(line);
                }
            }
        }  catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
