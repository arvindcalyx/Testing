package com.test.safetyconnect.model

import java.io.Serializable

/**
 * Created by Ganesh Padhayay on 20/06/23.
 */
class ImageDetectionFeedbackRequestModel(
    var request_id: String? = null,
    var labels: EquipmentLabels? = null
) : Serializable

class EquipmentLabels(
    var Ladder: Boolean? = null,
    var Helmet: Boolean? = null,
    var Safety_Harness: Boolean? = null,
    var Safety_Jacket: Boolean? = null,
    var Worker: Boolean? = null,
    var Bike_Helmet: Boolean? = null
) : Serializable
