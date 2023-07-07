package com.elconguyenvuong.pocketrunner.activity.home

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.elconguyenvuong.pocketrunner.R
import com.elconguyenvuong.pocketrunner.server.Server
import com.elconguyenvuong.pocketrunner.utils.AsyncRequest
import com.elconguyenvuong.pocketrunner.utils.saveTo
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity(), Handler.Callback {
    private var assemblies: HashMap<String, JSONObject>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val host: NavHostFragment = fragment as NavHostFragment

        val navController = host.navController
        navigation.setupWithNavController(navController)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            init()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        val appDirectoryPath = applicationInfo.dataDir
        val externalDirectory = applicationContext.getExternalFilesDir("PocketMine-MP")!!.path;

        Server.makeInstance(Server.Files(
                dataDirectory = File(externalDirectory),
                phar = File(externalDirectory, "PocketMine-MP.phar"),
                appDirectory = File(appDirectoryPath),
                php = File(appDirectoryPath, "php"),
                settingsFile = File(externalDirectory, "php.ini"),
                serverSetting = File(externalDirectory, "server.properties")
        ))

        try {
            val phpFile = File(Server.getInstance().files.php.toString())
            if (!phpFile.exists()) {
                copyAsset("php", phpFile.getAbsolutePath())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("php error", "exception")
        }
    }

    private fun init() {
        assemblies = AsyncRequest().execute("stable", "beta", "development").get()
        File(Server.getInstance().files.dataDirectory, "tmp").mkdirs()
        val ini = Server.getInstance().files.settingsFile
        if (!ini.exists()) {
            try {
                ini.createNewFile()
                ini.writeText("date.timezone=Asia/Ho_Chi_Minh\nzend.enable_gc=On\nzend.assertions=-1\n\nenable_dl=On\nallow_url_fopen=On\nmax_execution_time=0\nregister_argc_argv=On\n\nerror_reporting=-1\ndisplay_errors=stderr\ndisplay_startup_errors=On\n\ndefault_charset=\"UTF-8\"\n\nphar.readonly=Off\nphar.require_hash=On\n\nopcache.enable=1\nopcache.enable_cli=1\nopcache.save_comments=1\nopcache.load_comments=1\nopcache.fast_shutdown=0\nopcache.memory_consumption=128\nopcache.interned_strings_buffer=8\nopcache.max_accelerated_files=4000\nopcache.optimization_level=0xffffffff\n")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (assemblies != null && !Server.getInstance().isInstalled) {
            downloadPMBuild("stable")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init()
            } else {
                Toast.makeText(this, R.string.not_enough_rights, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun downloadFile(url: String, file: File) {
        if (file.exists()) file.delete()
        val view = View.inflate(this, R.layout.download,null)

        val builder = AlertDialog.Builder(this)
        builder
                .setTitle(getString(R.string.downloading).replace("%name%", file.name))
                .setCancelable(false)
                .setView(view)
        val dialog = builder.create()
        dialog.show()

        Thread(Runnable {
            try {
                URL(url).saveTo(file)
            } catch (_: Exception) {
                runOnUiThread {
                    Snackbar.make(content, R.string.download_error, Snackbar.LENGTH_LONG).show()
                }
            }
            runOnUiThread {
                if (dialog.isShowing) dialog.dismiss()
            }
        }).start()

    }

    override fun handleMessage(message: Message): Boolean {
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.download -> downloadPM()
            R.id.kill -> Server.getInstance().kill()
        }
        return super.onOptionsItemSelected(item)
    }

    @Throws(IOException::class)
    private fun copyAsset(path: String, to: String): File {
        val file = File(to)
        val input = applicationContext.assets.open(path)
        val output = FileOutputStream(file)
        input.copyTo(output)
        input.close()
        output.close()
        return file
    }

    private fun downloadPM() {
        if (assemblies == null) {
            Snackbar.make(content, R.string.assemblies_error, Snackbar.LENGTH_LONG).show()
            return
        } else if (Server.getInstance().isRunning) {
            Server.getInstance().kill()
        }

        val builds = assemblies!!.keys.toTypedArray()

        AlertDialog.Builder(this)
                .setTitle(R.string.select_channel)
                .setItems(builds) { _, index ->
                    val channel = builds[index]
                    val json = assemblies!![channel]
                    try {
                        val view = View.inflate(this, R.layout.build_info,null)

                        if (json!!.getBoolean("is_dev")) {
                            view.findViewById<TextView>(R.id.development_build).visibility = View.VISIBLE
                        }

                        view.findViewById<TextView>(R.id.api).text = json.getString("base_version")
                        view.findViewById<TextView>(R.id.build_number).text = json.getString("build_number")
                        view.findViewById<TextView>(R.id.branch).text = json.getString("branch")
                        view.findViewById<TextView>(R.id.game_version).text = json.getString("mcpe_version")

                        AlertDialog.Builder(this)
                                .setTitle(R.string.build_info)
                                .setView(view)
                                .setPositiveButton(R.string.download) { _, _ ->
                                    downloadPMBuild(channel)
                                }
                                .create()
                                .show()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }.create().show()
    }

    private fun downloadPMBuild(channel: String) {
        try {
            downloadFile(assemblies!![channel]!!.getString("download_url"), Server.getInstance().files.phar)
        } catch (e: Exception) {
            Snackbar.make(content, R.string.assemblies_error, Snackbar.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
