package com.dew.edward.picasso

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.coroutines.experimental.Ref
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import java.io.BufferedInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onClick(v: View?) {
        startChronometer()
        imageView.setImageResource(android.R.color.transparent)
        when(v?.id){
            R.id.buttonPicasso -> loadImageUsingPicasso()
            R.id.buttonGlide ->  loadImageUsingGlide()
            R.id.buttonVolley -> loadImageUsingVolley()
            R.id.buttonOkHttp -> loadImageUsingOkHttp()
            R.id.buttonHttp ->downloadBackground()
        }
    }

    private val requestQueue by lazy {
        Volley.newRequestQueue(this)
    }
    private val okClient by lazy { OkHttpClient() }
    private val okRequest by lazy {
        Request.Builder()
                .url(ImageUrl)
                .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonPicasso.setOnClickListener(this)
        buttonGlide.setOnClickListener(this)
        buttonVolley.setOnClickListener(this)
        buttonOkHttp.setOnClickListener(this)
        buttonHttp.setOnClickListener(this)

//        {
//            startChronometer()
//            imageView.setImageResource(android.R.color.transparent)
//
//            loadImageUsingPicasso()
//            loadImageUsingGlide()
//            loadImageUsingVolley()
//            loadImageUsingOkHttp()
//            downloadBackground()
//        }
    }

    private fun loadImageUsingHttpUrlConnection(): Bitmap? {
        val url = URL(ImageUrl)
        val urlConnection = url.openConnection() as HttpURLConnection
        var bitmap: Bitmap? = null

        try {
            val inputStream = BufferedInputStream(urlConnection.inputStream)
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException){
            e.printStackTrace()
        } finally {
            urlConnection.disconnect()
        }
        return bitmap
    }

    private fun downloadBackground(){
        val weakRef: Ref<MainActivity> = this.asReference()
        async(UI){
            val data : Deferred<Bitmap?> = bg{  // background thread
                loadImageUsingHttpUrlConnection()
            }
            val bitmap = data.await()
            if (bitmap != null){
                weakRef().imageView.setImageBitmap(bitmap)
                weakRef().chronometer.stop()
            }
        }
    }


    private fun loadImageUsingOkHttp(){
        okClient.newCall(okRequest)
                // enqueue make sure the downloading happens in background
                .enqueue(object : Callback{
                    override fun onFailure(call: Call?, e: IOException?) {
                        e?.printStackTrace()
                     }

                    override fun onResponse(call: Call?, response: okhttp3.Response?) {
                        val inputStream = response?.body()?.byteStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        // this function execute in background thread,
                        // so we need to indicate the following action should do in UiThread.
                        runOnUiThread {
                            imageView.setImageBitmap(bitmap)
                            chronometer.stop()
                        }

                    }
                })
    }
    private fun loadImageUsingVolley(){
        val imageRequest = ImageRequest(ImageUrl,
                Response.Listener { response ->
                    imageView.setImageBitmap(response)
                    chronometer.stop()
                },
                imageView.layoutParams.width, imageView.layoutParams.height,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.ARGB_8888,
                Response.ErrorListener { error ->
                    Toast.makeText(this,
                            "Could not download image.", Toast.LENGTH_SHORT).show()
                })

        requestQueue.add(imageRequest)

    }

    private fun loadImageUsingGlide(){
        GlideApp.with(this)
                .asBitmap()
                .load(Uri.parse(ImageUrl))
//                .into(imageView)
                .into(object : BitmapImageViewTarget(imageView){
                    override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                        super.onResourceReady(resource, transition)
                        chronometer.stop()
                    }
                })
    }

    private fun startChronometer() {
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
    }

    private fun loadImageUsingPicasso() {
        Picasso.with(this)
                .load(Uri.parse(ImageUrl))
                .into(imageView,
                        object : com.squareup.picasso.Callback {

                            override fun onSuccess() {
                                chronometer.stop()
                            }

                            override fun onError() {
                                Toast.makeText(applicationContext,
                                        "Could not download image.", Toast.LENGTH_LONG).show()
                            }
                        }
                )
    }


}
