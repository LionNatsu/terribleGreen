package com.example.lion.myapplication;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {
    private MyApplication myApp;
    protected Bitmap originalBmp = null;
    protected Bitmap greenBmp = null;
    protected ImageView mvo;
    protected ImageView mv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myApp = (MyApplication) getApplication();
        SeekBar sbQuality = (SeekBar) findViewById(R.id.seekBarQuality);
        assert sbQuality != null;
        SeekBar sbRepeat = (SeekBar) findViewById(R.id.seekBarRepeat);
        assert sbRepeat != null;
        sbQuality.setProgress(myApp.quality);
        sbRepeat.setProgress(myApp.repeatTimes);
        sbQuality.setOnSeekBarChangeListener(new qualityListener());
        sbRepeat.setOnSeekBarChangeListener(new timesListener());
        TextView t;
        t = (TextView)findViewById(R.id.textValueRepeat); assert t!=null;
        t.setText(String.valueOf(myApp.repeatTimes));
        t = (TextView)findViewById(R.id.textValueQuality); assert t!=null;
        t.setText(String.valueOf(myApp.quality));

        mvo = (ImageView) findViewById(R.id.imageViewOrigin);
        mv = (ImageView) findViewById(R.id.imageViewGreen);

        doUpdateOriginImage();
        doUpdateGreenImage();
        if (myApp.fileDescriptor == null) btOnClick(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void btOnClick(View dummy) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "选择打开一个图片"), 2333);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "请确保已安装有文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 2333:
                if (data == null) return;
                try {
                    myApp.fileDescriptor = getContentResolver().openAssetFileDescriptor(data.getData(), "r");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                doUpdateOriginImage();
                doUpdateGreenImage();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class timesListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            myApp.repeatTimes = progress;
            TextView t = (TextView)findViewById(R.id.textValueRepeat); assert t!=null;
            t.setText(String.valueOf(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            doUpdateGreenImage();
        }
    }

    private class qualityListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            myApp.quality = progress;
            TextView t = (TextView)findViewById(R.id.textValueQuality); assert t!=null;
            t.setText(String.valueOf(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            doUpdateGreenImage();
        }
    }

    public void doUpdateOriginImage() {
        if (myApp.fileDescriptor == null) return;
        originalBmp = getBmp();
        if (originalBmp == null) return;
        mvo.setImageBitmap(originalBmp);
        mv.setImageBitmap(Bitmap.createBitmap(originalBmp.getWidth(),originalBmp.getHeight(),originalBmp.getConfig()));
    }

    public void doUpdateGreenImage() {
        new Thread(new renderGreen()).start();
    }

    @Nullable
    private Bitmap getBmp() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
        if (layout == null) return null;
        int reqSize = layout.getWidth() * layout.getHeight();

        final BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(myApp.fileDescriptor.getFileDescriptor(), null, options);

        options.inSampleSize = computeSampleSize(options, -1, reqSize);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(myApp.fileDescriptor.getFileDescriptor(), null, options);
    }

    public static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }
        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;
        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(Math.floor(w / minSideLength), Math.floor(h / minSideLength));
        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }
        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    private class renderGreen implements Runnable {
        public void run() {
            greenBmp = originalBmp;
            if (originalBmp == null) return;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (int i = 0; i < myApp.repeatTimes; i++) {
                greenBmp.compress(Bitmap.CompressFormat.JPEG, myApp.quality, outputStream);
                greenBmp = BitmapFactory.decodeByteArray(outputStream.toByteArray(), 0, outputStream.size());
                outputStream.reset();
            }
            runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    mv.setImageBitmap(greenBmp);
                }
            });
        }
    }
}
