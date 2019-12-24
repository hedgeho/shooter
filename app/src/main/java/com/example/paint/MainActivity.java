package com.example.paint;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

import static com.example.paint.MainActivity.play;
import static com.example.paint.MainActivity.tv_score;
import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity {

    static AudioManager audioManager;
    static Button play;
    static TextView tv_score;
    PaintView paintView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        paintView = findViewById(R.id.main);
        tv_score = findViewById(R.id.score);
        play = findViewById(R.id.play);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (paintView.end) {
                    paintView.restart();
                    play.setText("Stop");
                    paintView.running = true;
                } else if (paintView.running) {
                    paintView.running = false;
                    play.setText("Play");
                } else {
                    paintView.running = true;
                    play.setText("Stop");
                }
            }
        });
    }


}

class PaintView extends SurfaceView {

    ArrayList<Point> list, targets;
    Paint paint;
    SurfaceHolder holder;
    Canvas canvas;
    SoundPool soundPool;
    int pool_id;
    boolean running = false;
    int center_x, center_y, speed = 10;
    int score = 0;
    boolean end = false;
    int hard = 0;

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);

    }

    public PaintView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    void restart() {
        list = new ArrayList<>();
        targets = new ArrayList<>();
        running = true;
        end = false;
        score = 0;
        tv_score.setText("Score: 0");
        hard = 0;
    }
    void init(Context context) {
        Log.e("mylog", "created");
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(100);
        holder = getHolder();
        soundPool = new SoundPool(100, AudioManager.STREAM_MUSIC, 100);
        pool_id = soundPool.load(getContext(), R.raw.future, 0);
        list = new ArrayList<>();
        targets = new ArrayList<>();
        //targets.add(new Point(700, 1500));

        @SuppressLint("HandlerLeak") final Handler h = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 90) {
                    tv_score.setText("Game over. Your score: " + score);
                    play.setText("Play again");
                    end = true;
                } else
                    tv_score.setText("Score: " + score);
            }
        };
        new Thread() {
            @Override
            public void run() {
                long prevTime = System.nanoTime();
                while(true) {
                    if (running) {
                        center_x = getWidth() / 2;
                        center_y = getHeight() / 2;
                        long time = System.nanoTime() - prevTime;
                        if (time < 1000000)
                            continue;
                        int[] del = new int[100];
                        for (int i = 0; i < 100; i++) {
                            del[i] = -1;
                        }
                        int i = 0;
                        boolean end = false;
                        for (Point target:targets) {
                            double sin = target.sin;
                            double cos = target.cos;
                            target.x = (int) (sin * (sqrt(sqr(target.y - center_y) + sqr(target.x - center_x)) - speed)) + center_x;
                            target.y = (int) (cos * (sqrt(sqr(target.y - center_y) + sqr(target.x - center_x)) - speed)) + center_y;

                            if(target.x - center_x <= 100 && target.x - center_x >= -100
                                    && target.y - center_y <= 100 && target.y - center_y >= -100) {
                                end = true;
                            }
                        }
                        if(end) {
                            h.sendEmptyMessage(90);
                            running = false;
                            continue;
                        }
                        ArrayList<Point> buf_list = new ArrayList<>(list);
                        for (Point p : buf_list) {
                            if (p.y == center_y) {
                                if (p.x > center_x) {
                                    p.x += speed;
                                } else if (p.x < center_x) {
                                    p.x -= speed;
                                }
                                continue;
                            }
                            //Log.e("mylog" ,"id: " + i);
                            //Log.e("mylog", "prev x: " + p.x + ", prev y: " + p.y);
                            double sin = p.sin;
                            double cos = p.cos;
                            //Log.e("mylog", "sin: " + sin + ", cos: " + cos);
                            p.x = (int) (sin * (sqrt(sqr(p.y - center_y) + sqr(p.x - center_x)) + speed)) + center_x;
                            p.y = (int) (cos * (sqrt(sqr(p.y - center_y) + sqr(p.x - center_x)) + speed)) + center_y;
                            if (p.x >= getWidth() || p.y >= getHeight() || p.x <= 0 || p.y <= 0) {
                                del[i] = 1;
                            }
                            int[] delt = new int[100];
                            for (int j = 0; j < 100; j++) {
                                delt[j] = -1;
                            }
                            int j = 0;
                            for (Point target : targets) {
                                if (p.x - target.x <= 100 && p.x - target.x >= -100
                                        && p.y - target.y <= 100 && p.y - target.y >= -100) {
                                    delt[j] = 1;
                                    del[i] = 1;
                                }
                                j++;
                            }
                            for (int k = 0, c = 0; k < 100; k++) {
                                if (delt[k] == -1)
                                    continue;
                                targets.remove(k - c);
                                score++;
                                h.sendEmptyMessage(1903);
                                c++;
                            }

                            //Log.e("mylog", "x: " + p.x + ", y: " + p.y);
                            i++;
                        }

                        for (int j = 0, c = 0; j < 100; j++) {
                            if(del[j] == -1)
                                continue;
                            list.remove(j-c);
                            c++;
                        }

                        canvas = holder.lockCanvas();
                        if(canvas != null) {
                            canvas.drawColor(Color.WHITE);
                            ArrayList<Point> buf = new ArrayList<>(list);
                            for (Point p : buf) {
                                canvas.drawPoint(p.x, p.y, paint);
                            }
                            paint.setColor(Color.GREEN);
                            buf = new ArrayList<>(targets);
                            for (Point t:buf) {
                                canvas.drawPoint(t.x, t.y, paint);
                            }
                            paint.setColor(Color.BLACK);
                            canvas.drawPoint(getWidth() / 2f, getHeight() / 2f, paint);
                            paint.setColor(Color.RED);
                            holder.unlockCanvasAndPost(canvas);
                        }
                        prevTime = System.nanoTime();
                    }
                }
            }
        }.start();

        new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (running) {
                        try {
                            Thread.sleep(1000 - hard);
                            hard+=2;
                            Log.e("mylog", "begin");
                            Random random = new Random();
                            int rand = Math.abs(random.nextInt() % 4) + 1;
                            int x, y;
                            switch (rand) {
                                case 1:
                                    x = Math.abs(random.nextInt() % getWidth() + 1);
                                    y = 0;
                                    break;
                                case 2:
                                    x = Math.abs(random.nextInt() % getWidth() + 1);
                                    y = getHeight();
                                    break;
                                case 3:
                                    x = 0;
                                    y = Math.abs(random.nextInt() % getHeight() + 1);
                                    break;
                                default:
                                    x = getWidth();
                                    y = Math.abs(random.nextInt() % getHeight() + 1);
                            }
                            Log.e("mylog", "x: " + x + ", y: " + y + ", center: x: " + center_x + ", y:" + center_y);
                            targets.add(new Point(x, y, center_x, center_y));
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }.start();
    }

    public PaintView(Context context) {
        super(context);
        init(context);

    }

    int sqr(int a) {
        return a*a;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN && running) {
            int x = (int)event.getX(), y = (int)event.getY();
            Log.e("mylog", "event x:" + x + ", y: " + y);
            double sin = (x - center_x) / sqrt(sqr(y - center_y) + sqr(x - center_x));
            double cos;
            if (y <= center_y)
                cos = -sqrt(1 - sin*sin);
            else
                cos = sqrt(1 - sin*sin);

            x = (int) (sin * 100) + center_x;
            y = (int) (cos * 100) + center_y;
            Log.e("mylog", "touch x: " + x + ", y: " + y + ", center: x: " + center_x + ", y:" + center_y);
            list.add(new Point(x, y, center_x, center_y));

            float leftVolume = 1.0f;
            float rightVolume = 1.0f;
            //soundPool.play(pool_id, leftVolume, rightVolume, 1, 0, 1.0f);


        }

        performClick();
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
class Point {
    int x, y;
    double sin, cos;

    Point(int x, int y, int center_x, int center_y) {
        this.x = x;
        this.y = y;
        if(center_x != -1) {
            sin = (x - center_x) / sqrt((y - center_y) * (y - center_y) + (x - center_x) * (x - center_x));
            if (y <= center_y)
                cos = -sqrt(1 - sin * sin);
            else
                cos = sqrt(1 - sin * sin);
        }
    }
    Point (int x, int y) {
        this(x, y, -1, -1);
    }
}