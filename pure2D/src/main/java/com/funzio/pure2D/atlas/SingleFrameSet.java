/*******************************************************************************
 * Copyright (C) 2012-2014 GREE, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
/**
 * 
 */
package com.funzio.pure2D.atlas;

import com.funzio.pure2D.gl.gl10.textures.Texture;

/**
 * @author long
 */
public class SingleFrameSet extends AtlasFrameSet {

    private AtlasFrame mFrame;
    private boolean mTextureLoaded;

    public SingleFrameSet(final String name, final Texture texture) {
        super(name);

        // only has 1 single frame
        addFrame(mFrame = new AtlasFrame(null, 0, name));

        // apply texture
        setTexture(texture);
    }

    @Override
    public void setTexture(final Texture texture) {
        super.setTexture(texture);

        // re-set the rect
        if (texture != null && texture.isLoaded()) {
            mTextureLoaded = true;

            mFrame.setRect(0, 0, (int) texture.getSize().x - 1, (int) texture.getSize().y - 1);

            // find the max frame size
            if (mFrame.mSize.x > mFrameMaxSize.x) {
                mFrameMaxSize.x = mFrame.mSize.x;
            }
            if (mFrame.mSize.y > mFrameMaxSize.y) {
                mFrameMaxSize.y = mFrame.mSize.y;
            }
        } else {
            mTextureLoaded = false;
        }
    }

    public boolean isTextureLoaded() {
        return mTextureLoaded;
    }

    @Override
    public String toString() {
        return String.format("SingleFrameSet( %s,\n%s )", mName, mFrame.toString());
    }
}