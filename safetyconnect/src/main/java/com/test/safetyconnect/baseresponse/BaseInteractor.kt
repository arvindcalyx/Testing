package com.test.safetyconnect.baseresponse


interface BaseInteractor {

    fun unbind()

    interface IResponseListener<T> {
        fun onProgress()

        fun onSuccess(resp: T)

        fun onFailure(t: Throwable)
    }
}
