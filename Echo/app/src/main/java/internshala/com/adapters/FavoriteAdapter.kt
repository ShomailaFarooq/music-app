package internshala.com.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import internshala.com.R
import internshala.com.Songs
import internshala.com.fragments.FavoriteFragment.Statified.mediaPlayer
import internshala.com.fragments.SongPlayingFragment

class FavoriteAdapter(_songDetails: ArrayList<Songs>, _context: Context) :
    RecyclerView.Adapter<FavoriteAdapter.MyViewHolder>() {
    var songDetails: ArrayList<Songs>? = null
    var mContext: Context? = null

    init {
        this.songDetails = _songDetails
        this.mContext = _context
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val mSongs = songDetails?.get(position)
        if (mSongs?.artist.equals("<unknown>", ignoreCase = true)) {
            holder.trackArtist?.setText("unknown")
        } else {
            holder.trackArtist?.setText(mSongs?.artist)
        }
        holder.trackTitle?.setText(mSongs?.songTitle)
        holder.contentHolder?.setOnClickListener(View.OnClickListener {
            try {
                if (mediaPlayer?.isPlaying() as Boolean) {
                    mediaPlayer?.stop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val args = Bundle()
            args.putString("path", mSongs?.songData)
            args.putString("songTitle", mSongs?.songTitle)
            args.putString("songArtist", mSongs?.artist)
            args.putInt("songPosition", position)
            args.putString("from", "favorite")
            args.putInt("SongId", mSongs?.songID?.toInt() as Int)
            args.putParcelableArrayList("songsData",songDetails)
            val songPlayingFragment = SongPlayingFragment()
            songPlayingFragment.arguments = args
            (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.details_fragment, songPlayingFragment)
                .addToBackStack("FavoriteToBackStack")
                .commit()
        })

    }

    override fun getItemCount(): Int {
        return songDetails?.size as Int
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_custom_mainscreen_adapter, parent, false)

        return MyViewHolder(itemView)

    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var trackTitle: TextView? = null
        var trackArtist: TextView? = null
        var contentHolder: RelativeLayout? = null

        init {
            trackTitle = view.findViewById<TextView>(R.id.trackTitle)
            trackArtist = view.findViewById<TextView>(R.id.trackArtist)
            contentHolder = view.findViewById<RelativeLayout>(R.id.contentRow)

        }
    }

}