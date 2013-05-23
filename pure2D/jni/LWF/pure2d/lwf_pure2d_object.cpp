#include <jni.h>
#include <android/log.h>
#include "lwf.h"
#include "lwf_pure2d_bitmap.h"
#include "lwf_pure2d_factory.h"

#define LOG_TAG "pure2d::LWF"
#define LOG(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

using namespace LWF;

struct DataContext {
    shared_ptr<Data> data;
    vector<shared_ptr<Pure2DRendererBitmapContext> > bitmapContexts;
    vector<shared_ptr<Pure2DRendererBitmapContext> > bitmapExContexts;

    DataContext() {}
    DataContext(shared_ptr<Data> d,
            vector<shared_ptr<Pure2DRendererBitmapContext> > &b,
            vector<shared_ptr<Pure2DRendererBitmapContext> > &bx)
        : data(d), bitmapContexts(b), bitmapExContexts(bx) {}
};

class EventHandlerWrapper {
private:
    JNIEnv *env;
    jobject object;
    jmethodID method;
    int handlerId;
public:
    EventHandlerWrapper(JNIEnv *e, jobject o, jmethodID m, int i)
        : env(e), object(o), method(m), handlerId(i) {}
    void operator()(Movie *, Button *) {
        env->CallIntMethod(object, method, handlerId);
    }
};

typedef map<int, DataContext> DataMap;
typedef map<int, shared_ptr<class LWF> > LWFMap;

static DataMap s_dataMap;
static int s_dataId;

static LWFMap s_lwfMap;
static int s_lwfId;

static vector<shared_ptr<Pure2DRendererBitmapContext> > s_nullContexts;

extern "C" JNIEXPORT jint JNICALL Java_com_funzio_pure2D_lwf_LWFData_create(JNIEnv *env, jobject obj, jbyteArray jdata)
{
    jsize len = env->GetArrayLength(jdata);
    jbyte *b = (jbyte *)env->GetByteArrayElements(jdata, NULL);

    shared_ptr<Data> data = make_shared<Data>(b, len);
    int id;
    if (data->Check()) {
        vector<shared_ptr<Pure2DRendererBitmapContext> > bitmapContexts;
        bitmapContexts.resize(data->bitmaps.size());
        for (size_t i = 0; i < data->bitmaps.size(); ++i) {
            const Format::Bitmap &b = data->bitmaps[i];
            if (b.textureFragmentId == -1)
                continue;

            Format::BitmapEx bx;
            bx.matrixId = b.matrixId;
            bx.textureFragmentId = b.textureFragmentId;
            bx.u = 0;
            bx.v = 0;
            bx.w = 1;
            bx.h = 1;

            bitmapContexts[i] =
                make_shared<Pure2DRendererBitmapContext>(data.get(), bx);
        }

        vector<shared_ptr<Pure2DRendererBitmapContext> > bitmapExContexts;
        bitmapExContexts.resize(data->bitmapExs.size());
        for (size_t i = 0; i < data->bitmapExs.size(); ++i) {
            const Format::BitmapEx &bx = data->bitmapExs[i];
            if (bx.textureFragmentId == -1)
                continue;

            bitmapExContexts[i] =
                make_shared<Pure2DRendererBitmapContext>(data.get(), bx);
        }

        id = ++s_dataId;
        s_dataMap[id] = DataContext(data, bitmapContexts, bitmapExContexts);
    } else {
        id = -1;
    }

    env->ReleaseByteArrayElements(jdata, b, 0);
    return id;
}

extern "C" JNIEXPORT jint JNICALL Java_com_funzio_pure2D_lwf_LWFData_getTextureNum(JNIEnv *env, jobject obj, jint jLWFDataId)
{
    DataMap::iterator it = s_dataMap.find((int)jLWFDataId);
    if (it == s_dataMap.end())
        return 0;

    return (jint)it->second.data->textures.size();
}

extern "C" JNIEXPORT jstring JNICALL Java_com_funzio_pure2D_lwf_LWFData_getTextureName(JNIEnv *env, jobject obj, jint jLWFDataId, jint jNo)
{
    DataMap::iterator it = s_dataMap.find((int)jLWFDataId);
    if (it == s_dataMap.end())
        return 0;

    const Format::Texture &t = it->second.data->textures[(int)jNo];
    string name = t.GetFilename(it->second.data.get());
    return env->NewStringUTF(name.c_str());
}

extern "C" JNIEXPORT void JNICALL Java_com_funzio_pure2D_lwf_LWFData_setGLTextureId(JNIEnv *env, jobject obj, jint jLWFDataId, jintArray jGLTextureIds)
{
    DataMap::iterator it = s_dataMap.find((int)jLWFDataId);
    if (it == s_dataMap.end())
        return;

    jsize len = env->GetArrayLength(jGLTextureIds);
    jint *ids = (jint *)env->GetIntArrayElements(jGLTextureIds, NULL);

    vector<shared_ptr<Pure2DRendererBitmapContext> >::iterator cit, citend;
    cit = it->second.bitmapContexts.begin();
    citend = it->second.bitmapContexts.end();
    for (; cit != citend; ++cit) {
        if (!*cit)
            continue;
        int id = (*cit)->GetTextureId();
        if (id >= 0)
            (*cit)->SetGLTextureId((int)ids[id]);
    }
    cit = it->second.bitmapExContexts.begin();
    citend = it->second.bitmapExContexts.end();
    for (; cit != citend; ++cit) {
        if (!*cit)
            continue;
        int id = (*cit)->GetTextureId();
        if (id >= 0)
            (*cit)->SetGLTextureId((int)ids[id]);
    }

    env->ReleaseIntArrayElements(jGLTextureIds, ids, 0);
}

extern "C" JNIEXPORT void JNICALL Java_com_funzio_pure2D_lwf_LWFData_destroy(JNIEnv *env, jobject obj, jint jLWFDataId)
{
    s_dataMap.erase((int)jLWFDataId);
}

extern "C" JNIEXPORT jint JNICALL Java_com_funzio_pure2D_lwf_LWF_create(JNIEnv *env, jobject obj, jint jLWFDataId)
{
    shared_ptr<class LWF> lwf;

    if ((int)jLWFDataId == INT_MAX) {

        shared_ptr<Data> data = make_shared<Data>();
        shared_ptr<Pure2DRendererFactory> factory =
            make_shared<Pure2DRendererFactory>(s_nullContexts, s_nullContexts);
        lwf = make_shared<class LWF>(data, factory);

    } else {

        DataMap::iterator it = s_dataMap.find((int)jLWFDataId);
        if (it == s_dataMap.end())
            return -1;

        shared_ptr<Pure2DRendererFactory> factory =
            make_shared<Pure2DRendererFactory>(
                it->second.bitmapContexts, it->second.bitmapExContexts);
        lwf = make_shared<class LWF>(it->second.data, factory);

    }

    int id = ++s_lwfId;
    s_lwfMap[id] = lwf;

    return id;
}

extern "C" JNIEXPORT jlong JNICALL Java_com_funzio_pure2D_lwf_LWF_getPointer(JNIEnv *env, jobject obj, jint jLWFId)
{
    LWFMap::iterator it = s_lwfMap.find((int)jLWFId);
    if (it == s_lwfMap.end())
        return 0;

    return (jlong)it->second.get();
}

extern "C" JNIEXPORT void JNICALL Java_com_funzio_pure2D_lwf_LWF_exec(JNIEnv *env, jobject obj, jlong jLWF, jfloat jTick)
{
    if (!jLWF)
        return;

    class LWF *lwf = (class LWF *)jLWF;
    lwf->Exec(jTick);
}

extern "C" JNIEXPORT void JNICALL Java_com_funzio_pure2D_lwf_LWF_render(JNIEnv *env, jobject obj, jlong jLWF)
{
    if (!jLWF)
        return;

    class LWF *lwf = (class LWF *)jLWF;
    lwf->Render();
}

extern "C" JNIEXPORT void JNICALL Java_com_funzio_pure2D_lwf_LWF_attachLWF(JNIEnv *env, jobject obj, jlong jLWF, jint jChildId, jstring jTarget, jstring jAttachName)
{
    if (!jLWF)
        return;

    LWFMap::iterator it = s_lwfMap.find((int)jChildId);
    if (it == s_lwfMap.end())
        return;

    class LWF *lwf = (class LWF *)jLWF;
    shared_ptr<class LWF> &child = it->second;

    const char *target = env->GetStringUTFChars(jTarget, 0);
    const char *attachName = env->GetStringUTFChars(jAttachName, 0);

    Movie *movie = lwf->SearchMovieInstance(target);
    if (movie)
        movie->AttachLWF(child, attachName);

    env->ReleaseStringUTFChars(jTarget, target);
    env->ReleaseStringUTFChars(jAttachName, attachName);
}

extern "C" JNIEXPORT int JNICALL Java_com_funzio_pure2D_lwf_LWF_addEventHandler(JNIEnv *env, jobject obj, jlong jLWF, jstring jEvent, jint jHandlerId)
{
    if (!jLWF)
        return -1;

    class LWF *lwf = (class LWF *)jLWF;

    const char *event = env->GetStringUTFChars(jEvent, 0);
    jclass cls = env->GetObjectClass(obj);
    jmethodID method = env->GetMethodID(cls, "callHandler", "(I)V");

    EventHandlerWrapper h =
        EventHandlerWrapper(env, obj, method, (int)jHandlerId);
    int id = lwf->AddEventHandler(event, h);

    env->ReleaseStringUTFChars(jEvent, event);

    return id;
}

extern "C" JNIEXPORT void JNICALL Java_com_funzio_pure2D_lwf_LWF_gotoAndPlay(JNIEnv *env, jobject obj, jlong jLWF, jstring jTarget, jstring jLabel)
{
    const char *target = env->GetStringUTFChars(jTarget, 0);
    const char *label = env->GetStringUTFChars(jLabel, 0);

    class LWF *lwf = (class LWF *)jLWF;
    Movie *movie = lwf->SearchMovieInstance(target);
    if (movie)
        movie->GotoAndPlay(label);

    env->ReleaseStringUTFChars(jTarget, target);
    env->ReleaseStringUTFChars(jLabel, label);
}

extern "C" JNIEXPORT void JNICALL Java_com_funzio_pure2D_lwf_LWF_gotoFrameAndPlay(JNIEnv *env, jobject obj, jlong jLWF, jstring jTarget, jint jFrame)
{
    const char *target = env->GetStringUTFChars(jTarget, 0);

    class LWF *lwf = (class LWF *)jLWF;
    Movie *movie = lwf->SearchMovieInstance(target);
    if (movie)
        movie->GotoAndPlay((int)jFrame);

    env->ReleaseStringUTFChars(jTarget, target);
}

extern "C" JNIEXPORT void JNICALL Java_com_funzio_pure2D_lwf_LWF_moveTo(JNIEnv *env, jobject obj, jlong jLWF, jstring jTarget, jfloat jX, jfloat jY)
{
    const char *target = env->GetStringUTFChars(jTarget, 0);

    class LWF *lwf = (class LWF *)jLWF;
    Movie *movie = lwf->SearchMovieInstance(target);
    if (movie)
        movie->MoveTo((float)jX, (float)jY);

    env->ReleaseStringUTFChars(jTarget, target);
}

extern "C" JNIEXPORT void JNICALL Java_com_funzio_pure2D_lwf_LWF_destroy(JNIEnv *env, jobject obj, jint jLWFId)
{
    s_lwfMap.erase((int)jLWFId);
}

