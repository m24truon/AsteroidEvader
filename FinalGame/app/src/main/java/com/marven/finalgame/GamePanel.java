package com.marven.finalgame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;


public class GamePanel extends SurfaceView implements SurfaceHolder.Callback
{
    public static final int WIDTH = 600;
    public static final int HEIGHT = 315;
    public static final int MOVESPEED = -5;
    private long smokeStartTime;
    private long asteroidStartTime;
    private MainThread thread;
    private Background bg;
    private Player player;
    private ArrayList<Smoke> smoke;
    private ArrayList<Asteroid> asteroid;
    private Random rand = new Random();

    private Explosion explosion;
    private long startReset;
    private boolean reset;
    private boolean disappear;
    private boolean started;
    private boolean newGameCreated;
    private int best;

    public GamePanel(Context context)
    {
        super(context);


        //add the callback to the surfaceholder to intercept events
        getHolder().addCallback(this);

        thread = new MainThread(getHolder(), this);

        //make gamePanel focusable so it can handle events
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        boolean retry = true;
        int counter = 0;
        while(retry && counter<1000)
        {
            counter++;
            try{thread.setRunning(false);
                thread.join();
                retry = false;
                thread = null;

            }catch(InterruptedException e){e.printStackTrace();}

        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){

        bg = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.spacebackground1));
        player = new Player(BitmapFactory.decodeResource(getResources(), R.drawable.spaceship2), 32, 32, 1);
        smoke = new ArrayList<Smoke>();
        asteroid = new ArrayList<Asteroid>();
        smokeStartTime = System.nanoTime();
        asteroidStartTime = System.nanoTime();



        thread.setRunning(true);
        thread.start();

    }
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            if(!player.getPlaying() && newGameCreated && reset)
            {
                player.setPlaying(true);
                player.setUp(true);
            }
            if(player.getPlaying())
            {
                if(!started) started = true;
                reset = false;
                player.setUp(true);
            }
            return true;
        }
        if(event.getAction()==MotionEvent.ACTION_UP)
        {
            player.setUp(false);
            return true;
        }

        return super.onTouchEvent(event);
    }

    public void update()
    {
        if(player.getPlaying()) {

            bg.update();
            player.update();

            long asteroidElapsed = (System.nanoTime() - asteroidStartTime)/1000000;
            if(asteroidElapsed >(2000 - player.getScore()/4))
            {
                if(asteroid.size() == 0)
                {
                    asteroid.add(new Asteroid(BitmapFactory.decodeResource(getResources(), R.drawable.boulder), WIDTH + 10, HEIGHT/2, 32, 32, player.getScore(), 1));
                }
                else
                {
                    asteroid.add(new Asteroid(BitmapFactory.decodeResource(getResources(), R.drawable.boulder), WIDTH + 10, (int)(rand.nextDouble()*((HEIGHT))), 32, 32, player.getScore(), 1));
                }
                asteroidStartTime = System.nanoTime();
            }

            for (int i = 0; i < asteroid.size(); i++)
            {
                asteroid.get(i).update();
                if(collision(asteroid.get(i), player))
                {
                    asteroid.remove(i);
                    player.setPlaying(false);
                    break;
                }
                if(asteroid.get(i).getX() < -100)
                {
                    asteroid.remove(i);
                    break;
                }
            }

            long elapsed = (System.nanoTime() - smokeStartTime)/1000000;
            if(elapsed > 120){
                smoke.add(new Smoke(player.getX(), player.getY()+10));
                smokeStartTime = System.nanoTime();
            }

            for(int i = 0; i<smoke.size();i++)
            {
                smoke.get(i).update();
                if(smoke.get(i).getX()<-10)
                {
                    smoke.remove(i);
                }
            }
        }
        else{
            player.resetDY();
            if(!reset)
            {
                newGameCreated = false;
                startReset = System.nanoTime();
                reset = true;
                disappear = true;
                explosion = new Explosion(BitmapFactory.decodeResource(getResources(), R.drawable.explosion), player.getX(), player.getY() - 30, 100, 100, 25);

            }
            explosion.update();
            long resetElapsed = (System.nanoTime() - startReset)/1000000;
            if(resetElapsed > 2500 && !newGameCreated)
            {
                newGame();
            }

            if(!newGameCreated) {
                newGame();
            }
        }
    }

    public boolean collision(GameObject a, GameObject b)
    {
        if (Rect.intersects(a.getRectangle(), b.getRectangle()))
        {
            return  true;
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas)
    {
        final float scaleFactorX = getWidth()/(WIDTH*1.f);
        final float scaleFactorY = getHeight()/(HEIGHT*1.f);

        if(canvas!=null) {
            final int savedState = canvas.save();


            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            if(!disappear){
                player.draw(canvas);
            }
            player.draw(canvas);
            for(Smoke sp: smoke)
            {
                sp.draw(canvas);
            }
            for(Asteroid a: asteroid)
            {
                a.draw(canvas);
            }

            if(started)
            {
                explosion.draw(canvas);
            }
            drawText(canvas);

            canvas.restoreToCount(savedState);

        }
    }
    public void newGame()
    {
        disappear = false;
        asteroid.clear();
        smoke.clear();

        player.resetDY();
        player.resetScore();
        player.setY(HEIGHT/2);

        if(player.getScore() > best)
        {
            best = player.getScore();
        }

        newGameCreated = true;
    }

    public void drawText(Canvas canvas)
    {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Score: " + (player.getScore() * 3), 10, HEIGHT - 10, paint);


        if(!player.getPlaying() && newGameCreated && reset)
        {
            Paint paint1 = new Paint();
            paint1.setColor(Color.WHITE);
            paint1.setTextSize(40);
            paint1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("ASTEROID EVASION", WIDTH / 2 - 160, HEIGHT / 2 - 60, paint1);
            canvas.drawText("TAP TO START", WIDTH / 2 - 100, HEIGHT / 2, paint1);

            paint1.setTextSize(20);
            canvas.drawText("Tap to go up", WIDTH / 2 - 100, HEIGHT / 2 + 20, paint1);
            canvas.drawText("Release to go down", WIDTH/2 - 100, HEIGHT/2 + 40, paint1);
            canvas.drawText("Dodge the asteroids!", WIDTH/2 - 100, HEIGHT/2 + 60, paint1);
        }
    }

}