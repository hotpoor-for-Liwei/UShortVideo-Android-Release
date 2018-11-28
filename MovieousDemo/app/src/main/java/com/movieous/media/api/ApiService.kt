package com.movieous.media.api

import io.reactivex.Observable
import retrofit2.http.GET

/**
 * Api 接口
 */
interface ApiService {

    @GET("v1/queries/list")
    fun getVideoList(): Observable<ArrayList<String>>

}