package com.wanderwildwood.ibasho.ui.access

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import com.wanderwildwood.ibasho.ui.theme.AppTheme

/**
 * Simple wrapper class to use FmdPermissionDialog from traditional XML views.
 */
class FmdPermissionDialogFragment : DialogFragment() {

    interface Listener {
        fun onPermissionCancelClicked()
        fun onPermissionSaveClicked(newValue: Long)
    }

    companion object {
        private const val ARG_INITIAL_PERMISSION = "initial_permission"

        fun newInstance(initialPermission: Long): FmdPermissionDialogFragment {
            return FmdPermissionDialogFragment().apply {
                arguments = Bundle().apply { putLong(ARG_INITIAL_PERMISSION, initialPermission) }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val initialPermission = requireArguments().getLong(ARG_INITIAL_PERMISSION)
        val listener = parentFragment as? Listener
            ?: requireActivity() as? Listener
            ?: error("Host must implement FmdPermissionDialogFragment.Listener")

        return Dialog(context).apply {
            // This shell only hosts the Compose dialog, which draws its own frame. Left as it
            // is, the shell adds a second window with a dimmed backdrop (a screen of grey).
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(0f)
            }
            setContentView(ComposeView(context).apply {
                setContent {
                    AppTheme {
                        FmdPermissionDialog(
                            initialPermission,
                            onCancelClicked = { listener.onPermissionCancelClicked() },
                            onSaveClicked = { listener.onPermissionSaveClicked(it) },
                        )
                    }
                }
            })
        }
    }

}
