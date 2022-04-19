package com.nemika.lyricsnmusic

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nemika.lyricsnmusic.data.LyricsData
import com.nemika.lyricsnmusic.data.YoutubeData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private var downloadable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup
        findViewById<TextView>(R.id.lyrics).movementMethod = ScrollingMovementMethod()
        val githubView: ImageView = findViewById(R.id.githubImage)
        githubView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nemika-Haj/LyricsNMusic"))
            startActivity(intent)
        }

        // Other
        val getButton: Button = findViewById(R.id.fetchLyricsButton)

        getButton.setOnClickListener {
            val view = this.currentFocus!!
            val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(view.windowToken, 0)
            getLyrics()
        }

        val downloadButton: Button = findViewById(R.id.downloadButton)
        downloadButton.setOnClickListener {
            if (this.downloadable) {
                val songName: String = findViewById<TextView>(R.id.songTitle).text.toString()
                val query: String = findViewById<EditText>(R.id.songInput).text.toString()
                createFile("$songName [LYRICS].lrc", query)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if(resultCode == RESULT_OK && data != null) {
                val uri: Uri = data.data!!
                val os = contentResolver.openOutputStream(uri)!!
                val lyrics: String = findViewById<TextView>(R.id.lyrics).text.toString()
                os.write(lyrics.toByteArray())
                os.close()
                Toast.makeText(this, "Lyrics Downloaded!", Toast.LENGTH_SHORT).show()

                val songName: String = findViewById<TextView>(R.id.songTitle).text.toString()

                val notification = NotificationCompat.Builder(this@MainActivity, "DOWNLOADED_NOTIFICATION")
                    .setContentTitle("Lyrics Downloaded")
                    .setContentText("Lyrics for $songName were downloaded!")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSmallIcon(androidx.core.R.drawable.notification_action_background)

                with(NotificationManagerCompat.from(this@MainActivity)) {
                    notify(0, notification.build())
                }
            }
        }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun createFile(fileName: String, query: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/lrc"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        startActivityForResult(intent, 1)

        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.bytestobits.dev/")
            .build()
            .create(ApiInterface::class.java)

        val downloadVideoSwitch: Switch = findViewById(R.id.download_video)

        if (downloadVideoSwitch.isChecked) {
            val youtubeDataFetch = retrofitBuilder.fetchYoutubeData(query)
            youtubeDataFetch.enqueue(object : Callback<YoutubeData?> {
                override fun onResponse(call: Call<YoutubeData?>, response: Response<YoutubeData?>) {
                    Log.d("YOUTUBE DATA", response.body().toString())

                    try {
                        val body = response.body()!!

                        val request = DownloadManager.Request(Uri.parse(body.download))
                        request.setTitle(body.title)
                        request.setDescription("Downloading Music")
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "${body.title}.mp3")

                        val downloadManager: DownloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                        downloadManager.enqueue(request)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@MainActivity, "Could not download music video!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<YoutubeData?>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Could not download music video!", Toast.LENGTH_SHORT).show()
                }
            })
        }

    }



    private fun getLyrics() {
        var song: String = findViewById<TextView>(R.id.songInput).text.toString().lowercase()
        var author = ""

        if (song.contains(" by ")) {
            val songAuthor = song.split(" by ")
            song = songAuthor[0]
            author = songAuthor[1]
        }

        val retrofitBuilder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.bytestobits.dev/")
            .build()
            .create(ApiInterface::class.java)

        val lyricFetch = retrofitBuilder.fetchLyrics(song, author)

        lyricFetch.enqueue(object : Callback<LyricsData?> {
            override fun onResponse(call: Call<LyricsData?>, response: Response<LyricsData?>) {
                try {
                    val body = response.body()!!
                    findViewById<TextView>(R.id.songTitle).text = body.title
                    findViewById<TextView>(R.id.lyrics).text = body.lyrics

                    this@MainActivity.downloadable = true

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, "Could not fetch lyrics, try again later!", Toast.LENGTH_SHORT).show()
                }
            }

            @SuppressLint("SimpleDateFormat")
            override fun onFailure(call: Call<LyricsData?>, t: Throwable) {
                val filePath = getExternalFilesDir("ErrorLogs")

                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("MM-dd-yyyy-k-m-s-S")
                val date = dateFormat.format(calendar.time)

                val file = File(filePath, "$date.log")
                val fos = FileOutputStream(file)
                fos.write(t.stackTrace.toString().toByteArray())
                fos.flush()
                fos.close()

                Toast.makeText(this@MainActivity, "Could not fetch lyrics, error log was created!", Toast.LENGTH_SHORT).show()
            }
        })
    }
}