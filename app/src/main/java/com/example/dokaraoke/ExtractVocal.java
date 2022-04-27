package com.example.dokaraoke;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.MultimediaObject;
import it.sauronsoftware.jave.MyDefaultFFMPEGLocator;



public class ExtractVocal extends AsyncTask<String, String, String> {
    private static long Wav_header_size;
    private final String Url;
    @SuppressLint("StaticFieldLeak")
    private final Context context;
    public static AlertDialog progressDialog;
    String extractKey = null;




    public ExtractVocal(String url, final Context ctxt, AlertDialog pd) {
        Url = url;
        context = ctxt;
        progressDialog = pd;
        

    }


    @SuppressLint("SetTextI18n")
    @Override
    protected void onPreExecute() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast toast = Toast.makeText(MyApp.getContext(), "Extracting Vocal...", Toast.LENGTH_SHORT);
            toast.show();
        });

        ShowProgress progress = new ShowProgress(progressDialog, "Vocal Extracting...");
        progress.execute();
        super.onPreExecute();
    }



    
    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("WrongThread")
    @Override
    protected String doInBackground(String... strings) {
        if(FFMPEG.getInstance(context).isSupported()) {

            File targetFile = null;
            File destinationFile = null;
            byte[] targetArr = new byte[0];
            try {
                targetFile = convertToWav(Url);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast toast = Toast.makeText(MainActivity2.getContext(), "Wav file generated!", Toast.LENGTH_SHORT);
                    toast.show();
                });
            } catch (EncoderException | IOException e) {
                e.printStackTrace();
            }
            try {
                getHEaderInfo(targetFile);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast toast = Toast.makeText(MainActivity2.getContext(), "Header collected!", Toast.LENGTH_SHORT);
                    toast.show();
                });
            } catch (IOException e) {
                extractKey = "error";
                e.printStackTrace();
            }
            try {
                destinationFile = copyHeader(targetFile);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast toast = Toast.makeText(MainActivity2.getContext(), "Header copied!", Toast.LENGTH_SHORT);
                    toast.show();
                });
            } catch (IOException e) {
                extractKey = "error";
                e.printStackTrace();
            }
            try {
                assert targetFile != null;
                targetArr = readThroughFile(targetFile);

                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast toast = Toast.makeText(MainActivity2.getContext(), "Data Array collected!", Toast.LENGTH_SHORT);
                    toast.show();
                });
            } catch (IOException e) {
                extractKey = "error";
                e.printStackTrace();
            }

            try {
                finalizeAudioData(destinationFile, targetArr);
                extractKey = "yes";
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast toast = Toast.makeText(MainActivity2.getContext(), "Audio Data finalized!", Toast.LENGTH_SHORT);
                    toast.show();
                });
            } catch (IOException e) {
                extractKey = "error";
                e.printStackTrace();
            }

        }
        else{
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast toast = Toast.makeText(context, "FFMPEG not supported!!!", Toast.LENGTH_SHORT);
                toast.show();
            });
        }
        return "done";
    }



    protected void onPostExecute(String s) {
        ShowProgress.dialogDismiss();
        if (extractKey==null){
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast toast = Toast.makeText(context, "Encoder or Source file issue!", Toast.LENGTH_SHORT);
                toast.show();
            });
        }
        else{
            if (extractKey.equals("nowav")){
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast toast = Toast.makeText(context, "Wav file issue!", Toast.LENGTH_SHORT);
                    toast.show();
                });
            }
            if (extractKey.equals("error")){
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast toast = Toast.makeText(context, "Vocal could not be extracted!", Toast.LENGTH_SHORT);
                    toast.show();
                });
            }
            else{
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast toast = Toast.makeText(context,"Vocal Extraction Complete!", Toast.LENGTH_SHORT);
                    toast.show();
                });
            }
        }
        super.onPostExecute("hi");


    }


    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }




    @Override
    protected void onCancelled() {
        try {
            handleCancelMethod();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleCancelMethod() throws IOException {
        extractKey = null;
        cancel(true);
        //finishAffinity();

    }
    

    private File convertToWav(String url) throws EncoderException, IOException {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS+"/doKaraoke/");
        String audioName = URLUtil.guessFileName(url, null, null);
        File source = new File(url);
        File targetFile = null;
         if (source.exists()){
            String finalFileName;
            finalFileName = audioName.replace(audioName.substring(audioName.lastIndexOf(".")),".wav");
            targetFile = new File(path, finalFileName);
            if(targetFile.exists()){
                targetFile.delete();
            }


            Uri outUri = Uri.fromFile(targetFile);
            targetFile = new File(outUri.getPath());


            Log.d("myTag2", String.valueOf(source));
            Log.d("myTag2", String.valueOf(targetFile));
            Log.d("myTag2", String.valueOf(audioName));

            final Integer bitRate = 16;
            final Integer samplingRate = 44100;//8000;
            final Integer channels = 2;
            final String codec = "pcm_s16le";
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec(codec);
            audio.setChannels(channels);
            audio.setBitRate(bitRate);
          //  audio.setVolume(400);
            audio.setSamplingRate(samplingRate);
            Encoder encoder = new Encoder(new MyDefaultFFMPEGLocator());
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("wav");
            attrs.setAudioAttributes(audio);


            try {
                encoder.encode(new MultimediaObject(source), targetFile, attrs);
                extractKey = "yes";
                Log.d("myTag1", "working");
            } catch (EncoderException e) {
                Log.d("myTag1", "not working");
                extractKey = "nowav";
                e.printStackTrace();
            }

        }


        return targetFile;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getHEaderInfo(File source) throws IOException {

        printWaveDescriptors(source);

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private File copyHeader(File source) throws IOException {
        File path =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS+"/doKaraoke/result/");
        if(!path.exists())
            path.mkdir();
        String audioName = URLUtil.guessFileName(String.valueOf(Url),null,null);
        String finalFileName = audioName.replace(audioName.substring(audioName.lastIndexOf(".")),".wav");
        File file = new File(path, finalFileName);
        if(!file.exists())
            file.createNewFile();
        Uri uri = Uri.fromFile(file);
        File destinationFile;
        destinationFile = new File(uri.getPath());
       while(destinationFile.exists()) {
            destinationFile.delete();
        }

        RandomAccessFile filein;
        RandomAccessFile fileout;

        byte[] header = new byte[(int) Wav_header_size];
        Log.d("myTag1", String.valueOf(Wav_header_size));
        filein = new RandomAccessFile(source,"r");
        fileout = new RandomAccessFile(destinationFile, "rw");
        filein.read(header);
        fileout.write(header,0, header.length);

        filein.close();
        fileout.close();
        System.out.println("header size" + destinationFile.length());

        return destinationFile;
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void printWaveDescriptors(File file)
            throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] bytes = new byte[4];
            long index = 0;


            if (in.read(bytes) < 0) {
                return;
            }

            index = index + bytes.length;
            printDescriptor(bytes, index);

            // First subchunk will always be at byte 12.
            // (There is no other dependable constant.)
            in.skip(8);
            index = index + 8;

            for (;;) {
                // Read each chunk descriptor.
                if (in.read(bytes) < 0) {
                    break;
                }
                index = index + bytes.length;
                printDescriptor(bytes, index);

                // Read chunk length.
                if (in.read(bytes) < 0) {
                    break;
                }

                // Skip the length of this chunk.
                // Next bytes should be another descriptor or EOF.
                int length = (
                        Byte.toUnsignedInt(bytes[0])
                                | Byte.toUnsignedInt(bytes[1]) << 8
                                | Byte.toUnsignedInt(bytes[2]) << 16
                                | Byte.toUnsignedInt(bytes[3]) << 24
                );
                index = index + Integer.toUnsignedLong(length);
                System.out.println("chunk length" + length);
                in.skip(Integer.toUnsignedLong(length));
                if(length == 0)
                    break;

            }

            System.out.println("End of file.");

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void printDescriptor(byte[] bytes, long index) {

        String desc = new String(bytes, StandardCharsets.US_ASCII);
        if (desc.equals("data")) {
            for (int i = 0; i < 4; i++, index++) {
                System.out.println("Found Data indexes " + index);
            }

            Wav_header_size =  index + 4;
            System.out.println("Found Header Size " + Wav_header_size);
        }


        System.out.println("Found " + desc + " descriptor.");

    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        // Get the size of the file
        long length = file.length() - Wav_header_size;

        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;

        is.skip(Wav_header_size);
        while (offset < bytes.length
                && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            is.close();
            throw new IOException("Could not completely read file "+file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }




    @RequiresApi(api = Build.VERSION_CODES.O)
    private byte[] readThroughFile(File source) throws IOException {

        return getBytesFromFile(source);



    }




    @RequiresApi(api = Build.VERSION_CODES.O)
    private void finalizeAudioData(File destinationFile, byte[] arr) throws IOException {

        FileOutputStream output = new FileOutputStream(destinationFile,true);

        for (int i = 0; i < arr.length; i += 4) {

                arr[i] = (byte) ~arr[i];
                arr[i + 1] = (byte) ~arr[i + 1];
                arr[i + 2] = arr[i + 2];
                arr[i + 3] = arr[i + 3];

        }

        copyData(arr,output);
        output.close();

    }


    private void copyData(byte [] arr, FileOutputStream out){
        try{
            out.write(arr, 0, arr.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}