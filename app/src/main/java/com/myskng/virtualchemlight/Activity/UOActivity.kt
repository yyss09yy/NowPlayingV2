package com.myskng.virtualchemlight.Activity

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewPropertyAnimator
import android.widget.ImageView
import butterknife.BindView
import butterknife.ButterKnife
import com.myskng.virtualchemlight.R
import com.myskng.virtualchemlight.UO.UOSensor
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.math.absoluteValue

class UOActivity : AppCompatActivity() {
    @BindView(R.id.UOimageViewMAX)
    lateinit var uoImageViewMAX: ImageView
    @BindView(R.id.UOimageViewNormal)
    lateinit var uoImageViewNormal: ImageView
    @BindView(R.id.UOimageViewOFF)
    lateinit var uoImageViewOFF: ImageView

    private lateinit var uoSensor: UOSensor
    private lateinit var uoSound: MediaPlayer
    private lateinit var vibrator: Vibrator

    private var uoLock: Boolean = false
    private val uoAnimatorList: MutableList<ViewPropertyAnimator> = mutableListOf()

    private val onGestureListener: GestureDetector.SimpleOnGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            //null check
            if (e1 == null || e2 == null) return super.onFling(e1, e2, velocityX, velocityY)
            //log
            Log.i("UO", "velocityX:${velocityX.absoluteValue}")
            Log.i("UO", "velocityY:${velocityY.absoluteValue}")
            Log.i("UO", "distanceX:${if (e2.x > e1.x) e2.x - e1.x else e1.x - e2.x}")
            Log.i("UO", "distanceY:${if (e2.y > e1.y) e2.y - e1.y else e1.y - e2.y}")
            //detect left right swipe
            val distX = if (e2.x > e1.x) e2.x - e1.x else e1.x - e2.x
            val distY = if (e2.y > e1.y) e2.y - e1.y else e1.y - e2.y
            val detected = when (true) {
                distY > 300 -> false
                velocityX > 10000 -> true
                distX > 700 -> true
                distX > 300 && velocityX > 3000 -> true
                else -> false
            }
            if (detected) onUODispose()
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uo)
        //ButterKnife
        ButterKnife.bind(this)
        //Gesture
        gestureDetector = GestureDetector(this, onGestureListener)
        //Prepare resource
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        uoSound = MediaPlayer.create(this, R.raw.uosound)
        uoSensor = UOSensor(this)
        uoSensor.startSensor()
        uoSensor.onUOIgnition = { onUOIgnition() }
        uoImageViewMAX.alpha = 0f
        uoImageViewNormal.alpha = 0f
    }

    private fun onUOIgnition() {
        launch(UI) {
            //check if locked
            if (uoLock) return@launch
            uoLock = true
            //stop animation
            uoAnimatorList.forEach { it ->
                it.cancel()
            }
            uoAnimatorList.clear()
            //reset alpha
            uoImageViewMAX.alpha = 0f
            uoImageViewNormal.alpha = 0f
            //start UO
            uoSound.start()
            //vibrate
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, 255))
            }
            Log.i("UO", "UO START!!")
            //uo effect
            suspendCoroutine<Unit> {
                with(uoImageViewMAX.animate().alpha(1.0f)) {
                    uoAnimatorList.add(this)
                    this.duration = 500
                    this.withEndAction { it.resume(Unit) }
                }
            }
            suspendCoroutine<Unit> {
                uoImageViewNormal.alpha = 1.0f
                with(uoImageViewMAX.animate().alpha(0.0f)) {
                    uoAnimatorList.add(this)
                    this.duration = 60 * 1000
                    this.withEndAction { it.resume(Unit) }
                }
            }
            suspendCoroutine<Unit> {
                with(uoImageViewNormal.animate().alpha(0.0f)) {
                    uoAnimatorList.add(this)
                    this.duration = 60 * 2 * 1000
                    this.withEndAction { it.resume(Unit) }
                }
            }
            uoLock = false
            Log.i("UO", "UO END!!")
        }
    }

    private fun onUODispose() {
        //stop animation
        uoAnimatorList.forEach { it ->
            it.cancel()
        }
        uoAnimatorList.clear()
        //reset alpha with animation
        with(uoImageViewMAX.animate().alpha(0.0f)) {
            this.duration = 500
            this.withEndAction { uoLock = false }
            uoAnimatorList.add(this)
        }
        with(uoImageViewNormal.animate().alpha(0.0f)) {
            this.duration = 500
            this.withEndAction { uoLock = false }
            uoAnimatorList.add(this)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        uoSensor.stopSensor()
    }

    override fun onResume() {
        super.onResume()
        uoSensor.startSensor()
    }
}