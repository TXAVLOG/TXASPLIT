/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAGroupsFragment.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import ke.txasplit.vk.TXAGroupDetailActivity
import ke.txasplit.vk.txa

class TXAGroupsFragment : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root = LinearLayout(requireContext())
        root.orientation = LinearLayout.VERTICAL
        root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        root.setPadding(24)
        root.gravity = Gravity.TOP

        val netBalance = TextView(requireContext()).apply {
            textSize = 18f
            text = txa("txasplit_dashboard_net_balance", "0")
        }

        val hint = TextView(requireContext()).apply {
            textSize = 14f
            text = txa("txasplit_groups_hint_open_sample")
        }

        val sample = TextView(requireContext()).apply {
            textSize = 16f
            text = txa("txasplit_groups_sample_group")
            setPadding(0, 24, 0, 0)
            setOnClickListener {
                startActivity(
                    Intent(requireContext(), TXAGroupDetailActivity::class.java)
                        .putExtra("group_name", txa("txasplit_groups_sample_group"))
                )
            }
        }

        root.addView(netBalance)
        root.addView(hint)
        root.addView(sample)
        return root
    }
}
