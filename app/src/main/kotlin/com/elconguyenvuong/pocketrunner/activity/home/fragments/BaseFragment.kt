package com.elconguyenvuong.pocketrunner.activity.home.fragments

import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.elconguyenvuong.pocketrunner.R

abstract class BaseFragment : Fragment() {
    protected fun handleError(error: Throwable) {
        if (activity == null) return

        activity!!.runOnUiThread {
            if (view != null) {
                Snackbar.make(view!!, R.string.unknown_errop, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
