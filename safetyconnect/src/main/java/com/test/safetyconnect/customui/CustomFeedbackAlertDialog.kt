package com.test.safetyconnect.customui

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.test.safetyconnect.R
import com.test.safetyconnect.model.EquipmentLabels
import com.test.safetyconnect.model.ImageDetectionFeedbackRequestModel
import com.test.safetyconnect.model.UploadImageResponse

class CustomFeedbackAlertDialog(
    activity: Activity?, private val isBikeHelmetCase: Boolean = false, private val requestId: String? = null, private val imageResponse: UploadImageResponse, private val responseCallback_: ((ImageDetectionFeedbackRequestModel?) -> Unit)?
) : Dialog(activity!!) {

    private var lytHarness: RelativeLayout? = null
    private var lytSafetyHelmet: RelativeLayout? = null
    private var lytLadder: RelativeLayout? = null
    private var lytSafetyJacket: RelativeLayout? = null
    private var lytWorker: RelativeLayout? = null
    private var lytBikeHelmet: RelativeLayout? = null

    private var tvHarness: TextView? = null
    private var tvSafetyHelmet: TextView? = null
    private var tvLadder: TextView? = null
    private var tvSafetyJacket: TextView? = null
    private var tvWorker: TextView? = null
    private var tvBikeHelmet: TextView? = null

    private var btnHarnessCorrect: AppCompatButton? = null
    private var btnSafetyHelmetCorrect: AppCompatButton? = null
    private var btnLadderCorrect: AppCompatButton? = null
    private var btnSafetyJacketCorrect: AppCompatButton? = null
    private var btnWorkerCorrect: AppCompatButton? = null
    private var btnBikeHelmetCorrect: AppCompatButton? = null

    private var btnHarnessNotCorrect: AppCompatButton? = null
    private var btnSafetyHelmetNotCorrect: AppCompatButton? = null
    private var btnLadderNotCorrect: AppCompatButton? = null
    private var btnSafetyJacketNotCorrect: AppCompatButton? = null
    private var btnWorkerNotCorrect: AppCompatButton? = null
    private var btnBikeHelmetNotCorrect: AppCompatButton? = null

    private var btnSubmit: AppCompatButton? = null

    private var harnessFilled: Boolean = false
    private var harnessValue: Boolean = false

    private var safetyHelmetFilled: Boolean = false
    private var safetyHelmetValue: Boolean = false

    private var safetyJacketFilled: Boolean = false
    private var safetyJacketValue: Boolean = false

    private var bikeHelmetFilled: Boolean = false
    private var bikeHelmetValue: Boolean = false

    private var ladderFilled: Boolean = false
    private var ladderValue: Boolean = false

    private var workerFilled: Boolean = false
    private var workerValue: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.custom_feedback_alert)
        setCancelable(false)
        this.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onStart() {
        initDialog()
        super.onStart()
    }

    private fun initDialog() {
        lytHarness = findViewById(R.id.lytHarness)
        lytSafetyHelmet = findViewById(R.id.lytSafetyHelmet)
        lytLadder = findViewById(R.id.lytLadder)
        lytSafetyJacket = findViewById(R.id.lytSafetyJacket)
        lytWorker = findViewById(R.id.lytWorker)
        lytBikeHelmet = findViewById(R.id.lytBikeHelmet)

        tvHarness = findViewById(R.id.tvHarness)
        tvSafetyHelmet = findViewById(R.id.tvSafetyHelmet)
        tvLadder = findViewById(R.id.tvLadder)
        tvSafetyJacket = findViewById(R.id.tvSafetyJacket)
        tvWorker = findViewById(R.id.tvWorker)
        tvBikeHelmet = findViewById(R.id.tvBikeHelmet)

        btnHarnessCorrect = findViewById(R.id.btnHarnessCorrect)
        btnSafetyHelmetCorrect = findViewById(R.id.btnSafetyHelmetCorrect)
        btnLadderCorrect = findViewById(R.id.btnLadderCorrect)
        btnSafetyJacketCorrect = findViewById(R.id.btnSafetyJacketCorrect)
        btnWorkerCorrect = findViewById(R.id.btnWorkerCorrect)
        btnBikeHelmetCorrect = findViewById(R.id.btnBikeHelmetCorrect)

        btnHarnessNotCorrect = findViewById(R.id.btnHarnessNotCorrect)
        btnSafetyHelmetNotCorrect = findViewById(R.id.btnSafetyHelmetNotCorrect)
        btnLadderNotCorrect = findViewById(R.id.btnLadderNotCorrect)
        btnSafetyJacketNotCorrect = findViewById(R.id.btnSafetyJacketNotCorrect)
        btnWorkerNotCorrect = findViewById(R.id.btnWorkerNotCorrect)
        btnBikeHelmetNotCorrect = findViewById(R.id.btnBikeHelmetNotCorrect)

        btnSubmit = findViewById(R.id.btnSubmit)

        if (isBikeHelmetCase) {
            lytBikeHelmet?.visibility = View.VISIBLE
            lytSafetyHelmet?.visibility = View.GONE
            lytSafetyJacket?.visibility = View.GONE
            lytHarness?.visibility = View.GONE
            lytLadder?.visibility = View.GONE
            lytWorker?.visibility = View.GONE
        } else {
            lytBikeHelmet?.visibility = View.GONE
            lytSafetyHelmet?.visibility = View.VISIBLE
            lytSafetyJacket?.visibility = View.VISIBLE
            lytHarness?.visibility = View.VISIBLE
            lytLadder?.visibility = View.VISIBLE
            lytWorker?.visibility = View.VISIBLE
        }

        //set data
        tvHarness?.text = "Harness - " + imageResponse?.equipment?.Safety_Harness
        tvSafetyHelmet?.text = "Safety_Helmet - " + imageResponse?.equipment?.Helmet
        tvSafetyJacket?.text = "Safety_Jacket - " + imageResponse?.equipment?.Safety_Jacket
        tvWorker?.text = "Worker - " + imageResponse?.equipment?.Worker
        tvLadder?.text = "Ladder - " + imageResponse?.equipment?.Ladder
        tvBikeHelmet?.text = "Bike_Helmet - " + imageResponse?.equipment?.Bike_Helmet

        btnHarnessCorrect?.setOnClickListener {
            harnessFilled = true
            btnHarnessCorrect?.setTextColor(Color.YELLOW)
            btnHarnessNotCorrect?.setTextColor(Color.BLACK)
            harnessValue = true
        }
        btnHarnessNotCorrect?.setOnClickListener {
            harnessFilled = true
            btnHarnessNotCorrect?.setTextColor(Color.YELLOW)
            btnHarnessCorrect?.setTextColor(Color.BLACK)
            harnessValue = false
        }

        btnSafetyJacketCorrect?.setOnClickListener {
            safetyJacketFilled = true
            btnSafetyJacketCorrect?.setTextColor(Color.YELLOW)
            btnSafetyJacketNotCorrect?.setTextColor(Color.BLACK)
            safetyJacketValue = true
        }
        btnSafetyJacketNotCorrect?.setOnClickListener {
            safetyJacketFilled = true
            btnSafetyJacketNotCorrect?.setTextColor(Color.YELLOW)
            btnSafetyJacketCorrect?.setTextColor(Color.BLACK)
            safetyJacketValue = false
        }

        btnSafetyHelmetCorrect?.setOnClickListener {
            safetyHelmetFilled = true
            btnSafetyHelmetCorrect?.setTextColor(Color.YELLOW)
            btnSafetyHelmetNotCorrect?.setTextColor(Color.BLACK)
            safetyHelmetValue = true
        }
        btnSafetyHelmetNotCorrect?.setOnClickListener {
            safetyHelmetFilled = true
            btnSafetyHelmetNotCorrect?.setTextColor(Color.YELLOW)
            btnSafetyHelmetCorrect?.setTextColor(Color.BLACK)
            safetyHelmetValue = false
        }

        btnBikeHelmetCorrect?.setOnClickListener {
            bikeHelmetFilled = true
            btnBikeHelmetCorrect?.setTextColor(Color.YELLOW)
            btnBikeHelmetNotCorrect?.setTextColor(Color.BLACK)
            bikeHelmetValue = true
        }
        btnBikeHelmetNotCorrect?.setOnClickListener {
            bikeHelmetFilled = true
            btnBikeHelmetNotCorrect?.setTextColor(Color.YELLOW)
            btnBikeHelmetCorrect?.setTextColor(Color.BLACK)
            bikeHelmetValue = false
        }

        btnWorkerCorrect?.setOnClickListener {
            workerFilled = true
            btnWorkerCorrect?.setTextColor(Color.YELLOW)
            btnWorkerNotCorrect?.setTextColor(Color.BLACK)
            workerValue = true
        }
        btnWorkerNotCorrect?.setOnClickListener {
            workerFilled = true
            btnWorkerNotCorrect?.setTextColor(Color.YELLOW)
            btnWorkerCorrect?.setTextColor(Color.BLACK)
            workerValue = false
        }

        btnLadderCorrect?.setOnClickListener {
            ladderFilled = true
            btnLadderCorrect?.setTextColor(Color.YELLOW)
            btnLadderNotCorrect?.setTextColor(Color.BLACK)
            ladderValue = true
        }
        btnLadderNotCorrect?.setOnClickListener {
            ladderFilled = true
            btnLadderNotCorrect?.setTextColor(Color.YELLOW)
            btnLadderCorrect?.setTextColor(Color.BLACK)
            ladderValue = false
        }

        btnSubmit?.setOnClickListener {
            if (validateInputs()) {
                val request = ImageDetectionFeedbackRequestModel()
                request.request_id = requestId
                val labels = EquipmentLabels()
                if (isBikeHelmetCase) {
                    labels.Bike_Helmet = bikeHelmetValue
                } else {
                    labels.Safety_Harness = harnessValue
                    labels.Safety_Jacket = safetyJacketValue
                    labels.Helmet = safetyHelmetValue
                    labels.Worker = workerValue
                    labels.Ladder = ladderValue
                }
                request.labels = labels
                responseCallback_?.invoke(request)
                dismiss()
            } else {
                responseCallback_?.invoke(null)
            }
        }
    }

    private fun validateInputs(): Boolean {
        return if (isBikeHelmetCase) {
            if (bikeHelmetFilled) {
                return true
            }
            false
        } else {
            if (harnessFilled && safetyHelmetFilled && safetyJacketFilled && workerFilled && ladderFilled) {
                return true
            }
            false
        }
    }
}