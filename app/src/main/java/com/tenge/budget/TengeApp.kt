package com.tenge.budget

import android.app.Application
import com.tenge.budget.di.AppContainer

class TengeApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
