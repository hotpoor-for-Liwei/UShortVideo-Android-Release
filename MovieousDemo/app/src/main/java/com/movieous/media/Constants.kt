package com.movieous.media

object Constants {
    // Only for debug, please change to your app`s authorization info.
    // See also: https://docs.ucloud.cn/storage_cdn/ufile/index
    const val BASE_URL = "https://api.movieous.cn"
    const val MOVIEOUS_SIGN = ""
    const val DOMAIN = "" // UCloud 存储空间域名
    const val PUBLIC_TOKEN = "" // 上传公钥
    const val PRIVATE_TOKEN = "" // 上传私钥
    const val BUCKET = "" // bucket 名称
    const val PROXY_SUFFIX = "" // 存储空间域名后缀
    const val MEDIA_FILE_PREFIX = "shortvideo" // 短视频文件前缀
    const val THUMB_FILE_PREFIX = "thumb"  // 视频封面文件前缀

    // 最大录制时长
    const val DEFAULT_MAX_RECORD_DURATION = 10 * 1000
    // 编码码率
    const val DEFAULT_ENCODING_BITRATE = 2500 * 1000

    // 存储目录设置
    const val VIDEO_STORAGE_DIR = "/sdcard/movieous/shortvideo/"
    const val RECORD_FILE_PATH = VIDEO_STORAGE_DIR + "record.mp4"
    const val TITLE_FILE_PATH = VIDEO_STORAGE_DIR + "title.mp4"
    const val TAIL_FILE_PATH = VIDEO_STORAGE_DIR + "tail.mp4"
    const val EDIT_FILE_PATH = VIDEO_STORAGE_DIR + "edit.mp4"
    const val COVER_FILE_PATH = VIDEO_STORAGE_DIR + "cover.gif"
    const val MERGE_FILE_PATH = VIDEO_STORAGE_DIR + "merge.mp4"

    // 变速录制/编辑参数
    const val VIDEO_SPEED_SUPER_SLOW = 0.25
    const val VIDEO_SPEED_SLOW = 0.5
    const val VIDEO_SPEED_NORMAL = 1
    const val VIDEO_SPEED_FAST = 2
    const val VIDEO_SPEED_SUPER_FAST = 4
}