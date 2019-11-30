package com.apkupdater.repository

import android.app.Activity
import android.app.AlertDialog
import com.apkupdater.R
import com.apkupdater.util.AppPreferences
import com.apkupdater.util.InstallUtil
import com.apkupdater.util.catchingAsync
import com.apkupdater.util.ioScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.gson.responseObject
import com.kryptoprefs.invoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SelfUpdateRepository: KoinComponent {

	private val installer: InstallUtil by inject()
	private val url = "http://rumboalla.github.io/apkupdater/version.json"
	private val prefs: AppPreferences by inject()
	private val interval = 24 * 60 * 60 * 1000

	fun checkForUpdatesAsync(activity: Activity) = ioScope.catchingAsync {
		if (System.currentTimeMillis() - prefs.selfUpdateCheck() > interval) {
			val response = Fuel.get(url).responseObject<SelfUpdateResponse>().third.get()
			prefs.selfUpdateCheck(System.currentTimeMillis())
			if (response.version > activity.packageManager.getPackageInfo(activity.packageName, 0).versionCode) {
				if (withContext(Dispatchers.Main) { showDialog(activity) }) {
					installer.install(activity, installer.downloadAsync2(activity, response.apk) { _, _ -> }, 0)
				}
			}
		}
	}

	private suspend fun showDialog(activity: Activity) = suspendCoroutine<Boolean> {
		AlertDialog.Builder(activity, R.style.AlertDialogTheme)
			.setMessage(activity.getString(R.string.selfupdate_found))
			.setPositiveButton(activity.getString(R.string.selfupdate_ok)) { _, _: Int -> it.resume(true) }
			.setNegativeButton(activity.getString(R.string.selfupdate_cancel)) { _, _ -> it.resume(false) }
			.create()
			.show()
	}

}

data class SelfUpdateResponse(val version: Int = 0, val apk: String = "")