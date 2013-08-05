package com.funzio.pure2D.demo.containers;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.funzio.pure2D.BaseScene;
import com.funzio.pure2D.demo.R;
import com.funzio.pure2D.demo.activities.StageActivity;
import com.funzio.pure2D.demo.objects.Bouncer;
import com.funzio.pure2D.demo.objects.SquareContainer;
import com.funzio.pure2D.gl.gl10.GLState;
import com.funzio.pure2D.gl.gl10.textures.Texture;

public class DisplayGroupActivity extends StageActivity {
    private List<Texture> mTextures = new ArrayList<Texture>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // need to get the GL reference first
        mScene.setListener(new BaseScene.Listener() {

            @Override
            public void onSurfaceCreated(final GLState glState, final boolean firstTime) {
                if (firstTime) {
                    // load the textures
                    loadTextures();

                    // generate a lot of squares
                    addGroup(mRandom.nextInt(mDisplaySize.x), mRandom.nextInt(mDisplaySize.y));
                }
            }
        });
    }

    private void loadTextures() {
        final int[] ids = {
                R.drawable.cc_32, // cc
                R.drawable.mw_32, // mw
                R.drawable.ka_32, // ka
        };

        for (int id : ids) {
            // add texture to list
            mTextures.add(mScene.getTextureManager().createDrawableTexture(id, null));
        }
    }

    private void addGroup(final float x, final float y) {
        // generate a lot of squares
        SquareContainer container = new SquareContainer();
        container.setSize(200, 200);
        // random positions
        container.setPosition(x, y);

        for (int n = 0; n < 5; n++) {
            int random = mRandom.nextInt(mTextures.size());
            Texture texture = mTextures.get(random);
            // create object
            Bouncer sq = new Bouncer();
            sq.setTexture(texture);
            // random positions
            sq.setPosition(mRandom.nextInt(200), mRandom.nextInt(200));

            // add to container
            container.addChild(sq);
        }

        // add to scene
        mScene.addChild(container);
    }

    @Override
    protected int getNumObjects() {
        return mScene.getNumGrandChildren();
    }

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mStage.queueEvent(new Runnable() {

                @Override
                public void run() {
                    addGroup(event.getX(), mDisplaySize.y - event.getY());
                }
            });
        }

        return true;
    }
}
