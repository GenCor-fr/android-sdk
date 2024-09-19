package tech.kissmyapps.android.appupdates

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber
import kotlin.system.exitProcess

class AppUpdateManager(context: Context) {
    private var _activity: Activity? = null

    private var appUpdateManager = AppUpdateManagerFactory.create(context)
    private var appUpdateResultLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    private val applicationVersionCode = getVersionCode(context)

    init {
        val application = context.applicationContext as Application
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (_activity != null && _activity == activity) {
                    return
                }

                _activity = activity

                onCreate(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                // do nothing
            }

            override fun onActivityResumed(activity: Activity) {
                if (_activity != activity) {
                    return
                }

                onResume(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                // do nothing
            }

            override fun onActivityStopped(activity: Activity) {
                // do nothing
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                // do nothing
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (_activity != activity) {
                    return
                }

                onDestroy(activity)
                _activity = null
            }
        })
    }

    fun setMinSupportedVersionCode(versionCode: Long) {
        if (applicationVersionCode == null || versionCode <= applicationVersionCode) {
            return
        }

        requestAppUpdateInfo()
    }

    private fun onCreate(activity: Activity) {
        if (activity !is ComponentActivity) {
            return
        }

        appUpdateResultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { activityResult ->
            if (activityResult.resultCode == RESULT_CANCELED) {
                activity.finishAffinity()
                exitProcess(0)
            } else if (activityResult.resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
                requestAppUpdateInfo()
            }
        }
    }

    private fun onResume(activity: Activity) {
        if (activity !is ComponentActivity) {
            return
        }

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                logAppUpdateInfo(appUpdateInfo)

                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    startUpdateFlow(appUpdateInfo)
                }
            }
            .addOnFailureListener { error ->
                Timber.e(error, "App update failed.")
            }
    }

    private fun onDestroy(activity: Activity) {
        if (activity !is ComponentActivity) {
            return
        }

        appUpdateResultLauncher?.unregister()
        appUpdateResultLauncher = null
    }

    private fun requestAppUpdateInfo() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                logAppUpdateInfo(appUpdateInfo)

                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    startUpdateFlow(appUpdateInfo)
                }
            }
            .addOnFailureListener { error ->
                Timber.e(error, "App update failed.")
            }
    }

    private fun startUpdateFlow(appUpdateInfo: AppUpdateInfo) {
        val activityResultLauncher = appUpdateResultLauncher ?: run {
            return
        }

        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            activityResultLauncher,
            AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)
        )
    }

    private fun logAppUpdateInfo(appUpdateInfo: AppUpdateInfo) {
        val updateAvailability = when (appUpdateInfo.updateAvailability()) {
            UpdateAvailability.UPDATE_AVAILABLE -> "UPDATE_AVAILABLE"
            UpdateAvailability.UPDATE_NOT_AVAILABLE -> "UPDATE_NOT_AVAILABLE"
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> "DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS"
            else -> "UNKNOWN"
        }

        Timber.d("Update availability: $updateAvailability")
    }

    private fun getVersionCode(context: Context): Int? {
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
                .versionCode
        } catch (e: Throwable) {
            Timber.e(e, "Failed to obtain application version code.")
            null
        }
    }
}