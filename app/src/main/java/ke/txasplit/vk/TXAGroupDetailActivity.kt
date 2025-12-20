/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAGroupDetailActivity.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import ke.txasplit.vk.databinding.TxaActivityGroupDetailBinding
import ke.txasplit.vk.ui.TXASimpleTextFragment
import ke.txasplit.vk.ui.adapter.TXATabPagerAdapter

class TXAGroupDetailActivity : AppCompatActivity() {

    private lateinit var vb: TxaActivityGroupDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = TxaActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.toolbar.setNavigationOnClickListener { finish() }

        val groupName = intent.getStringExtra("group_name")?.takeIf { it.isNotBlank() }
            ?: txa("txasplit_groups_sample_group")
        vb.toolbar.title = groupName

        val fragments = listOf(
            TXASimpleTextFragment.newInstance(txa("txasplit_group_tab_bills")),
            TXASimpleTextFragment.newInstance(txa("txasplit_group_tab_members")),
            TXASimpleTextFragment.newInstance(txa("txasplit_group_tab_stats")),
        )
        vb.pager.adapter = TXATabPagerAdapter(this, fragments)

        val titles = listOf(
            txa("txasplit_group_tab_bills"),
            txa("txasplit_group_tab_members"),
            txa("txasplit_group_tab_stats"),
        )

        TabLayoutMediator(vb.tabs, vb.pager) { tab, pos ->
            tab.text = titles.getOrNull(pos) ?: ""
        }.attach()
    }
}
