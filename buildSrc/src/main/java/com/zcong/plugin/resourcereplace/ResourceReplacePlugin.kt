package com.zcong.plugin.resourcereplace

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.api.BaseVariantImpl
import com.zcong.plugin.resourcereplace.utils.ImageUtil
import com.zcong.plugin.resourcereplace.utils.LogUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.awt.Color
import java.io.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.imageio.ImageIO


class ResourceReplacePlugin : Plugin<Project> {

    private lateinit var mcImageProject: Project
    private lateinit var mcImageConfig: Config
    private var oldSize: Long = 0
    private var newSize: Long = 0
    val bigImgList = ArrayList<String>()

    var isDebugTask = false
    var isContainAssembleTask = false

    override fun apply(project: Project) {

        mcImageProject = project

        //check is library or application
        val hasAppPlugin = project.plugins.hasPlugin("com.android.application")
        val variants = if (hasAppPlugin) {
            (project.property("android") as AppExtension).applicationVariants
        } else {
            (project.property("android") as LibraryExtension).libraryVariants
        }
        //set config
        project.extensions.create("ResourceReplaceConfig", Config::class.java)
        mcImageConfig = project.property("ResourceReplaceConfig") as Config

        project.gradle.taskGraph.whenReady {
            it.allTasks.forEach { task ->
                val taskName = task.name
                if (taskName.contains("assemble") || taskName.contains("resguard") || taskName.contains("bundle")) {
                    if (taskName.toLowerCase().endsWith("debug") &&
                            taskName.toLowerCase().contains("debug")) {
                        isDebugTask = true
                    }
                    isContainAssembleTask = true
                    return@forEach
                }
            }
        }

        project.afterEvaluate {
            variants.all { variant ->

                variant as BaseVariantImpl

                val mergeResourcesTask = variant.mergeResourcesProvider.get()
                val mcPicTask = project.task("McImage${variant.name.capitalize()}")

                mcPicTask.doLast {

                    //debug enable
//                    if (isDebugTask && !mcImageConfig.enableWhenDebug) {
//                        LogUtil.log("zhengcong Debug not run ^_^")
//                        return@doLast
//                    }

                    //assemble passed
//                    if (!isContainAssembleTask) {
//                        LogUtil.log("Don't contain assemble task, mcimage passed")
//                        return@doLast
//                    }

                    LogUtil.log("---- ResourceReplace Plugin Start ----")

                    val dir = variant.allRawAndroidResources.files

                    val cacheList = ArrayList<String>()

                    val imageFileList = ArrayList<File>()

                    for (channelDir: File in dir) {
                        traverseResDir(channelDir, imageFileList, cacheList)

                        for (image: File in imageFileList) {
                            //指定的图片路径
                            val bImage = ImageIO.read(FileInputStream(image.absolutePath))

                            //获取图片的长宽高
                            val width = bImage.width
                            val height = bImage.height
                            val minx = bImage.minTileX
                            val miny = bImage.minTileY
                            //    改变图片的颜色
                            val rgb = IntArray(3)
                            bImage.apply {
                                var x = minx
                                while(x < width) {
                                    var y = miny
                                    while(y < height) {
                                        val pixel = getRGB(x, y)
                                        rgb[0] = pixel and 0xff0000 shr 16
                                        rgb[1] = pixel and 0xff00 shr 8
                                        rgb[2] = pixel and 0xff
//                                        LogUtil.log("R=${rgb[0]}    G=${rgb[1]}    B=${rgb[2]}    ")
                                        if (rgb[0] == mcImageConfig.oColor[0] && rgb[1] == mcImageConfig.oColor[1] && rgb[2] == mcImageConfig.oColor[2]) {
                                            val red = Color(mcImageConfig.nColor[0], mcImageConfig.nColor[1], mcImageConfig.nColor[2])
                                            setRGB(x, y, red.rgb)
                                        }
                                        y += 1
                                    }
                                    x += 1
                                }
                            }
                            val type = image.name.substring(image.name.indexOf(".")+1,image.name.length)
//                            LogUtil.log("type.name=$type")
                            ImageIO.write(bImage, type, File(image.absolutePath))//参数分别是缓冲区图像，类型，文件名；

                            val picList = ArrayList<File>()

                            for (pic in mcImageConfig.picList) {
                                LogUtil.log("0000000000=" +pic)
                                val file = File(pic)
                                traversePicDir(file,picList)
                            }

                            for (pic in picList) {
                                if (image.name.equals(pic.name)) {
                                    LogUtil.log("11111111=" +pic.absolutePath)
                                    copyfile(pic.absolutePath,image.absolutePath)
                                    LogUtil.log("22222222=" +image.absolutePath)
                                }
                            }
                        }
                    }

                    val start = System.currentTimeMillis()
//
//                    mtDispatchOptimizeTask(imageFileList)
//                    LogUtil.log(sizeInfo())
                    LogUtil.log("---- ResourceReplace Plugin End ----, Total Time(ms) : ${System.currentTimeMillis() - start}")
                }

                //inject task
                (project.tasks.findByName(mcPicTask.name) as Task).dependsOn(mergeResourcesTask.taskDependencies.getDependencies(mergeResourcesTask))
                mergeResourcesTask.dependsOn(project.tasks.findByName(mcPicTask.name))

            }
        }

    }

    private fun traverseResDir(file: File, imageFileList: ArrayList<File>, cacheList: ArrayList<String>) {
        if (cacheList.contains(file.absolutePath)) {
            return
        } else {
            cacheList.add(file.absolutePath)
        }
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                if (it.isDirectory) {
                    traverseResDir(it, imageFileList, cacheList)
                } else {
                    filterImage(it, imageFileList)
                }
            }
        } else {
            filterImage(file, imageFileList)
        }
    }

    private fun traversePicDir(file: File, imageFileList: ArrayList<File>) {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                if (it.isDirectory) {
                    traversePicDir(it, imageFileList)
                } else {
                    imageFileList.add(it)
                }
            }
        } else {
            imageFileList.add(file)
        }
    }

    private fun filterImage(file: File, imageFileList: ArrayList<File>) {
        if (/*mcImageConfig.whiteList.contains(file.name) || */ImageUtil.isImage(file)) {
            /*if (*//*mcImageConfig.whiteList.contains(file.name) || *//*isContain(file.absolutePath)
                && ImageUtil.isImage(file)) {*/
            imageFileList.add(file)
        }

    }

    private fun isContain(path: String): Boolean {

        for (string in mcImageConfig.whiteList) {
            if (path.contains(string)) return true
        }
        return false
    }

    private fun filterString(file: File, stringFileList: ArrayList<File>) {
        if (mcImageConfig.whiteList.contains(file.name) || !ImageUtil.isImage(file)) {
            return
        }
        stringFileList.add(file)
    }

    private fun mtDispatchOptimizeTask(imageFileList: ArrayList<File>) {
        if (imageFileList.size == 0 || bigImgList.isNotEmpty()) {
            return
        }
        val coreNum = Runtime.getRuntime().availableProcessors()
        if (imageFileList.size < coreNum || !mcImageConfig.multiThread) {
            for (file in imageFileList) {
                optimizeImage(file)
            }
        } else {
            val results = ArrayList<Future<Unit>>()
            val pool = Executors.newFixedThreadPool(coreNum)
            val part = imageFileList.size / coreNum
            for (i in 0 until coreNum) {
                val from = i * part
                val to = if (i == coreNum - 1) imageFileList.size - 1 else (i + 1) * part - 1
                results.add(pool.submit(Callable<Unit> {
                    for (index in from..to) {
                        optimizeImage(imageFileList[index])
                    }
                }))
            }
            for (f in results) {
                try {
                    f.get()
                } catch (ignore: Exception) {
                }
            }
        }
    }

    private fun optimizeImage(file: File) {
        val path: String = file.path
        if (File(path).exists()) {
            oldSize += File(path).length()
        }
      /*  when (mcImageConfig.optimizeType) {
            Config.OPTIMIZE_WEBP_CONVERT ->
                WebpUtils.securityFormatWebp(file, mcImageConfig, mcImageProject)
            Config.OPTIMIZE_COMPRESS_PICTURE ->
                CompressUtil.compressImg(file)
        }*/
        countNewSize(path)
    }

    private fun countNewSize(path: String) {
        if (File(path).exists()) {
            newSize += File(path).length()
        } else {
            //转成了webp
            val indexOfDot = path.lastIndexOf(".")
            val webpPath = path.substring(0, indexOfDot) + ".webp"
            if (File(webpPath).exists()) {
                newSize += File(webpPath).length()
            } else {
                LogUtil.log("McImage: optimizeImage have some Exception!!!")
            }
        }
    }

    /**
     * 单个文件复制
     */
    fun copyfile(srcFile: String, destFile: String) {

/*        var fis = FileInputStream(srcFile);
        var fos = FileOutputStream(destFile)

        var bis = BufferedInputStream(fis)
        var bos = BufferedOutputStream(fos)


        var buf = ByteArray(1024)

        var len = 0;
        while (true) {
            len = bis.read(buf)
            if (len == -1) break;
            bos.write(buf, 0, len)
        }
        fis.close()
        fos.close()*/

        FileInputStream(srcFile).use { fis -> //将D盘的文件复制了一份
            FileOutputStream(destFile).use { fos ->
                val bis = fis.buffered()//创建字节缓冲输入流
                val bos = fos.buffered()//创建字节缓冲输出流
                bis.copyTo(bos) //将输入流数据拷贝到输出流去
                bos.flush()//关闭输出流
            }
        }
    }
}