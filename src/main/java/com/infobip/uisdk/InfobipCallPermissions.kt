package com.infobip.uisdk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

object InfobipCallPermissions {

  fun hasRequiredPermissions(activity: ComponentActivity): Boolean {
    return requiredPermissions().all { permission ->
      ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }
  }

  fun requestCallPermissions(activity: ComponentActivity) {
    val missing = requiredPermissions().filter { permission ->
      ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
    }
    if (missing.isEmpty()) {
      return
    }

    val launcher = activity.activityResultRegistry.register(
      "infobip_call_permissions_${activity.hashCode()}",
      ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    launcher.launch(missing.toTypedArray())
  }

  fun requiredPermissions(): List<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      add(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      add(Manifest.permission.BLUETOOTH_CONNECT)
    }
  }
}
