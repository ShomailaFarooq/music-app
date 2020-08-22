package internshala.com.fragments

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.cleveroad.audiovisualization.AudioVisualization
import com.cleveroad.audiovisualization.DbmHandler
import com.cleveroad.audiovisualization.GLAudioVisualizationView
import internshala.com.CurrentSongHelper
import internshala.com.R
import internshala.com.Songs
import internshala.com.databases.EchoDatabase
import internshala.com.fragments.SongPlayingFragment.Statified.currentPosition
import internshala.com.fragments.SongPlayingFragment.Statified.currentSongHelper
import internshala.com.fragments.SongPlayingFragment.Statified.endTimeText
import internshala.com.fragments.SongPlayingFragment.Statified.fab
import internshala.com.fragments.SongPlayingFragment.Statified.favoriteContent
import internshala.com.fragments.SongPlayingFragment.Statified.fetchSongs
import internshala.com.fragments.SongPlayingFragment.Statified.glView
import internshala.com.fragments.SongPlayingFragment.Statified.loopImageButton
import internshala.com.fragments.SongPlayingFragment.Statified.mediaPlayer
import internshala.com.fragments.SongPlayingFragment.Statified.myActivity
import internshala.com.fragments.SongPlayingFragment.Statified.nextImageButton
import internshala.com.fragments.SongPlayingFragment.Statified.playpauseImageButton
import internshala.com.fragments.SongPlayingFragment.Statified.previousImageButton
import internshala.com.fragments.SongPlayingFragment.Statified.seekbar
import internshala.com.fragments.SongPlayingFragment.Statified.shuffleImageButton
import internshala.com.fragments.SongPlayingFragment.Statified.songArtistView
import internshala.com.fragments.SongPlayingFragment.Statified.songTitleView
import internshala.com.fragments.SongPlayingFragment.Statified.startTimeText
import internshala.com.fragments.SongPlayingFragment.Statified.updateSongTime
import kotlinx.android.synthetic.main.fragment_song_playing.*
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * A simple [Fragment] subclass.
 *
 */
class SongPlayingFragment : Fragment() {
    object Statified {
        var myActivity: Activity? = null
        var mediaPlayer: MediaPlayer? = null
        var startTimeText: TextView? = null
        var endTimeText: TextView? = null
        var playpauseImageButton: ImageButton? = null
        var previousImageButton: ImageButton? = null
        var nextImageButton: ImageButton? = null
        var loopImageButton: ImageButton? = null
        var seekbar: SeekBar? = null
        var songArtistView: TextView? = null
        var songTitleView: TextView? = null
        var shuffleImageButton: ImageButton? = null
        var currentPosition: Int = 0
        var fetchSongs: ArrayList<Songs>? = null
        var currentSongHelper: CurrentSongHelper? = null
        var audioVisualization: AudioVisualization? = null
        var glView: GLAudioVisualizationView? = null

        var fab: ImageButton? = null

        var favoriteContent: EchoDatabase? = null
        var mSensorManager:SensorManager?=null
        var mSensorListener:SensorEventListener?=null
        var MY_PREFS_NAME="ShakeFeature"
        var currentTrackHelper = currentSongHelper?.songTitle

        var updateSongTime = object : Runnable {
            override fun run() {
                val getcurrent = mediaPlayer?.currentPosition
                startTimeText?.setText(
                    String.format(
                        "%d:%d",
                        TimeUnit.MILLISECONDS.toMinutes(getcurrent?.toLong() as Long),
                        TimeUnit.MILLISECONDS.toSeconds(getcurrent?.toLong() as Long) -
                                TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getcurrent?.toLong() as Long))
                    )
                )
                seekbar?.setProgress(getcurrent?.toInt() as Int)
                Handler().postDelayed(this, 1000)
            }

        }
    }

    object Staticated {
        var MY_PREFS_SHUFFLE = "Shuffle feature"
        var MY_PREFS_LOOP = "Loop feature"

        fun onSongComplete() {
            if (currentSongHelper?.isShuffle as Boolean) {
                playNext("PlayNextLikeNormalShuffle")
                currentSongHelper?.isPlaying = true
            } else {
                if (currentSongHelper?.isLoop as Boolean) {
                    currentSongHelper?.isPlaying = true
                    var nextSong = fetchSongs?.get(currentPosition)

                    currentSongHelper?.songTitle = nextSong?.songTitle
                    currentSongHelper?.songPath = nextSong?.songData
                    currentSongHelper?.currentPosition = currentPosition
                    currentSongHelper?.songId = nextSong?.songID as Long

                    updateTextViews(currentSongHelper?.songTitle as String, currentSongHelper?.songArtist as String)

                    mediaPlayer?.reset()
                    try {
                        mediaPlayer?.setDataSource(myActivity as Context, Uri.parse(currentSongHelper?.songPath))
                        mediaPlayer?.prepare()
                        mediaPlayer?.start()
                        processInformation(mediaPlayer as MediaPlayer)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else {
                    playNext("PlayNextNormal")
                    currentSongHelper?.isPlaying = true

                }
            }
            if (favoriteContent?.checkifIdExists(currentSongHelper?.songId?.toInt() as Int) as Boolean) {
                fab?.setImageDrawable(ContextCompat.getDrawable(myActivity as Context, R.drawable.favorite_on))
            } else {
                fab?.setImageDrawable(ContextCompat.getDrawable(myActivity as Context, R.drawable.favorite_off))
            }
        }

        fun updateTextViews(songtitle: String, songArtist: String) {
            var songTitleUpdated=songtitle
            var songArtistUpdated=songArtist
            if(songtitle.equals("<unknown>",true)){
                songTitleUpdated="unknown"
            }
            if(songArtist.equals("<unknown>",true)){
                songArtistUpdated="unknown"
            }
            songTitleView?.setText(songTitleUpdated)
            songArtistView?.setText(songArtistUpdated)


        }

        fun processInformation(mediaPlayer: MediaPlayer) {
            val finalTime = mediaPlayer.duration
            val startTime = mediaPlayer.currentPosition
            seekbar?.max = finalTime
            startTimeText?.setText(
                String.format(
                    "%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()))
                )
            )
            endTimeText?.setText(
                String.format(
                    "%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(finalTime.toLong()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()))
                )
            )
            seekbar?.setMax(finalTime)
            seekbar?.setProgress(startTime)
            Handler().postDelayed(updateSongTime, 1000)

        }

        fun playNext(check: String) {
            if (check.equals("PlayNextNormal", ignoreCase = true)) {
                currentPosition = currentPosition + 1
            } else if (check.equals("PlayNextLikeNormalShuffle", true)) {
                var randomObject = Random()
                var randomPosition = randomObject.nextInt(Statified.fetchSongs?.size?.plus(1) as Int)
                currentPosition = randomPosition
            }
            if (currentPosition == fetchSongs?.size) {
                currentPosition = 0
            }
            currentSongHelper?.isLoop = false
            var nextSong = fetchSongs?.get(currentPosition)
            currentSongHelper?.songTitle = nextSong?.songTitle
            currentSongHelper?.songPath = nextSong?.songData
            currentSongHelper?.currentPosition = currentPosition
            currentSongHelper?.songId = nextSong?.songID as Long

            updateTextViews(currentSongHelper?.songTitle as String, currentSongHelper?.songArtist as String)

            mediaPlayer?.reset()
            try {
                mediaPlayer?.setDataSource(myActivity as Context, Uri.parse(currentSongHelper?.songPath))
                mediaPlayer?.prepare()
                mediaPlayer?.start()
                processInformation(mediaPlayer as MediaPlayer)

            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (favoriteContent?.checkifIdExists(currentSongHelper?.songId?.toInt() as Int) as Boolean) {
                fab?.setImageDrawable(ContextCompat.getDrawable(myActivity as Context, R.drawable.favorite_on))
            } else {
                fab?.setImageDrawable(ContextCompat.getDrawable(myActivity as Context, R.drawable.favorite_off))
            }
        }

    }

    var mAccelaration:Float=0f
    var mAccelarationCurrent:Float=0f
    var mAccelarationLast:Float=0f


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view = inflater!!.inflate(R.layout.fragment_song_playing, container, false)
        setHasOptionsMenu(true)
        activity?.title="Now Playing"

        Statified.seekbar = view?.findViewById(R.id.seekBar)
        Statified.startTimeText = view?.findViewById(R.id.startTime)
        Statified.endTimeText = view?.findViewById(R.id.endTime)
        Statified.playpauseImageButton = view?.findViewById(R.id.playPauseButton)
        Statified.nextImageButton = view?.findViewById(R.id.nextButton)
        Statified.previousImageButton = view?.findViewById(R.id.previousButton)
        Statified.loopImageButton = view?.findViewById(R.id.loopButton)
        Statified.shuffleImageButton = view?.findViewById(R.id.shuffleButton)
        Statified.songArtistView = view?.findViewById(R.id.songArtist)
        Statified.songTitleView = view?.findViewById(R.id.songTitle)
        Statified.glView = view?.findViewById(R.id.visualizer_view)
        Statified.fab = view?.findViewById(R.id.favoriteIcon)
        Statified.fab?.alpha = 0.8f

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Statified.audioVisualization = glView as AudioVisualization
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Statified.myActivity = context as Activity

    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        Statified.myActivity = activity
    }

    override fun onResume() {
        super.onResume()
        Statified.audioVisualization?.onResume()
        Statified.mSensorManager?.registerListener(Statified.mSensorListener,Statified.mSensorManager?.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        Statified.audioVisualization?.onPause()
        super.onPause()
        Statified.mSensorManager?.unregisterListener(Statified.mSensorListener)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        Statified.audioVisualization?.release()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statified.mSensorManager=Statified.myActivity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelaration=0.0f
        mAccelarationCurrent=SensorManager.GRAVITY_EARTH
        mAccelarationLast=SensorManager.GRAVITY_EARTH
        bindShakeListener()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater?.inflate(R.menu.song_playing_menu,menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        val item:MenuItem?=menu?.findItem(R.id.action_redirect)
        item?.isVisible=true
        val item2:MenuItem?=menu?.findItem(R.id.action_sort)
        item2?.isVisible=false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.action_redirect ->{
                Statified.myActivity?.onBackPressed()
                return false
            }

        }
        return false
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        Statified.favoriteContent = EchoDatabase(Statified.myActivity)
        Statified.currentSongHelper = CurrentSongHelper()
        Statified.currentSongHelper?.isPlaying = true
        Statified.currentSongHelper?.isLoop = false
        Statified.currentSongHelper?.isShuffle = false

        var path: String? = null
        var _songTitle: String? = null
        var _songArtist: String? = null
        var songId: Long = 0
        try {
            path = arguments?.getString("path")
            _songTitle = arguments?.getString("songTitle")
            _songArtist = arguments?.getString("songArtist")
            songId = arguments?.getInt("SongId")?.toLong() as Long
            Statified.currentPosition = arguments?.getInt("songPosition") as Int
            Statified.fetchSongs = arguments?.getParcelableArrayList("songData")



            Statified.currentSongHelper?.songPath = path
            Statified.currentSongHelper?.songTitle = _songTitle
            Statified.currentSongHelper?.songArtist = _songArtist
            Statified.currentSongHelper?.songId = songId
            Statified.currentSongHelper?.currentPosition = Statified.currentPosition

            Staticated.updateTextViews(
                Statified.currentSongHelper?.songTitle as String,
                Statified.currentSongHelper?.songArtist as String
            )


        } catch (e: Exception) {
            e.printStackTrace()
        }

        var fromFavBottomBar = arguments?.get("FavBottomBar") as? String
        if (fromFavBottomBar != null) {
            Statified.mediaPlayer = FavoriteFragment.Statified.mediaPlayer

        } else {
            Statified.mediaPlayer = MediaPlayer()
            Statified.mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
            try {
                Statified.mediaPlayer?.setDataSource(Statified.myActivity as Context, Uri.parse(path))
                Statified.mediaPlayer?.prepare()

            } catch (e: Exception) {
                e.printStackTrace()
            }
            Statified.mediaPlayer?.start()
        }
        Staticated.processInformation(Statified.mediaPlayer as MediaPlayer)

        if (Statified.currentSongHelper?.isPlaying as Boolean) {
            Statified.playpauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
        } else {
            Statified.playpauseImageButton?.setBackgroundResource(R.drawable.play_icon)
        }
        Statified.mediaPlayer?.setOnCompletionListener {
            Staticated.onSongComplete()
        }

        clickHandler()
        var visualizationHandler = DbmHandler.Factory.newVisualizerHandler(Statified.myActivity as Context, 0)
        Statified.audioVisualization?.linkTo(visualizationHandler)

        var prefsForShuffle =
            Statified.myActivity?.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)
        var isShuffleAllowed = prefsForShuffle?.getBoolean("feature", false)
        if (isShuffleAllowed as Boolean) {
            Statified.currentSongHelper?.isShuffle = true
            Statified.currentSongHelper?.isLoop = false
            Statified.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_icon)
            Statified.loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
        } else {
            Statified.currentSongHelper?.isShuffle = false
            Statified.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
        }

        var prefsForLoop = Statified.myActivity?.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)
        var isLoopAllowed = prefsForLoop?.getBoolean("feature", false)
        if (isLoopAllowed as Boolean) {
            Statified.currentSongHelper?.isShuffle = false
            Statified.currentSongHelper?.isLoop = true
            Statified.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
            Statified.loopImageButton?.setBackgroundResource(R.drawable.loop_icon)
        } else {
            Statified.loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
            Statified.currentSongHelper?.isLoop = false
        }
        if (Statified.favoriteContent?.checkifIdExists(Statified.currentSongHelper?.songId?.toInt() as Int) as Boolean) {
            Statified.fab?.setImageDrawable(
                ContextCompat.getDrawable(
                    Statified.myActivity as Context,
                    R.drawable.favorite_on
                )
            )
        } else {
            Statified.fab?.setImageDrawable(
                ContextCompat.getDrawable(
                    Statified.myActivity as Context,
                    R.drawable.favorite_off
                )
            )
        }
    }

    fun clickHandler() {

        seekbar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBarget: SeekBar?) {
                seekbar?.setProgress(seekbar?.getProgress() as Int)
                mediaPlayer?.seekTo(seekbar?.getProgress() as Int)
            }
        })


        Statified.fab?.setOnClickListener({
            if (Statified.favoriteContent?.checkifIdExists(Statified.currentSongHelper?.songId?.toInt() as Int) as Boolean) {
                Statified.fab?.setImageDrawable(
                    ContextCompat.getDrawable(
                        Statified.myActivity as Context,
                        R.drawable.favorite_off
                    )
                )
                favoriteContent?.deleteFavourite(currentSongHelper?.songId?.toInt() as Int)
                Toast.makeText(myActivity, "Removed from favorites", Toast.LENGTH_SHORT).show()
            } else {
                fab?.setImageDrawable(ContextCompat.getDrawable(myActivity as Context, R.drawable.favorite_on))
                favoriteContent?.storeAsFavorite(
                    currentSongHelper?.songId?.toInt(), currentSongHelper?.songArtist,
                    currentSongHelper?.songTitle, currentSongHelper?.songPath
                )
                Toast.makeText(myActivity, "Added to favorites", Toast.LENGTH_SHORT).show()
            }
        })
        shuffleImageButton?.setOnClickListener({
            var editorShuffle =
                myActivity?.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)?.edit()
            var editorLoop = myActivity?.getSharedPreferences(Staticated.MY_PREFS_LOOP, Context.MODE_PRIVATE)?.edit()
            if (currentSongHelper?.isShuffle as Boolean) {
                shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
                currentSongHelper?.isShuffle = false
                editorShuffle?.putBoolean("feature", false)
                editorShuffle?.apply()
            } else {
                currentSongHelper?.isShuffle = true
                currentSongHelper?.isLoop = false
                shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_icon)
                loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
                editorShuffle?.putBoolean("feature", true)
                editorShuffle?.apply()
                editorLoop?.putBoolean("feature", false)
                editorLoop?.apply()
            }
        })
        nextImageButton?.setOnClickListener({
            currentSongHelper?.isPlaying = true
            Statified.playpauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
            if (currentSongHelper?.isShuffle as Boolean) {
                Staticated.playNext("PlayNextLikeNormalShuffle")
            } else {
                Staticated.playNext("PlayNextNormal")
            }


        })
        previousImageButton?.setOnClickListener({
            currentSongHelper?.isPlaying = true
            if (currentSongHelper?.isLoop as Boolean) {
                loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
            }
            playPrevious()

        })
        loopImageButton?.setOnClickListener({
            var editorShuffle =
                myActivity?.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)?.edit()
            var editorLoop = myActivity?.getSharedPreferences(Staticated.MY_PREFS_LOOP, Context.MODE_PRIVATE)?.edit()

            if (currentSongHelper?.isLoop as Boolean) {
                currentSongHelper?.isLoop = false
                loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
                editorLoop?.putBoolean("feature", false)
                editorLoop?.apply()
            } else {
                currentSongHelper?.isLoop = true
                currentSongHelper?.isShuffle = false
                loopImageButton?.setBackgroundResource(R.drawable.loop_icon)
                shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
                editorShuffle?.putBoolean("feature", false)
                editorShuffle?.apply()
                editorLoop?.putBoolean("feature", true)
                editorLoop?.apply()
            }
        })
        playpauseImageButton?.setOnClickListener({
            if (mediaPlayer?.isPlaying as Boolean) {
                mediaPlayer?.pause()
                currentSongHelper?.isPlaying = false
                playpauseImageButton?.setBackgroundResource(R.drawable.play_icon)
            } else {
                mediaPlayer?.start()
                currentSongHelper?.isPlaying = true
                playpauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
            }
        })
    }

    fun playPrevious() {
        currentPosition = currentPosition - 1
        if (currentPosition == -1) {
            currentPosition = 0
        }
        if (currentSongHelper?.isPlaying as Boolean) {
            playpauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
        } else {
            playpauseImageButton?.setBackgroundResource(R.drawable.play_icon)
        }
        currentSongHelper?.isLoop = false
        val nextSong = fetchSongs?.get(currentPosition)
        currentSongHelper?.songTitle = nextSong?.songTitle
        currentSongHelper?.songPath = nextSong?.songData
        currentSongHelper?.currentPosition = currentPosition
        currentSongHelper?.songId = nextSong?.songID as Long

        Staticated.updateTextViews(currentSongHelper?.songTitle as String, currentSongHelper?.songArtist as String)

        mediaPlayer?.reset()
        try {
            mediaPlayer?.setDataSource(activity as Context, Uri.parse(currentSongHelper?.songPath))
            mediaPlayer?.prepare()
            mediaPlayer?.start()
            Staticated.processInformation(mediaPlayer as MediaPlayer)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (favoriteContent?.checkifIdExists(currentSongHelper?.songId?.toInt() as Int) as Boolean) {
            fab?.setImageDrawable(ContextCompat.getDrawable(myActivity as Context, R.drawable.favorite_on))
        } else {
            fab?.setImageDrawable(ContextCompat.getDrawable(myActivity as Context, R.drawable.favorite_off))
        }

    }
    fun bindShakeListener(){
        Statified.mSensorListener= object : SensorEventListener {
            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            }

            override fun onSensorChanged(p0: SensorEvent) {
                val x= p0.values[0]
                val y= p0.values[1]
                val z= p0.values[2]

                mAccelarationLast=mAccelarationCurrent
                mAccelarationCurrent=Math.sqrt(((x*x + y*y + z*z).toDouble())).toFloat()
                val delta=mAccelarationCurrent-mAccelarationLast
                mAccelaration=mAccelaration * 0.9f + delta

                if(mAccelaration>12){
                    val prefs=Statified.myActivity?.getSharedPreferences(Statified.MY_PREFS_NAME,Context.MODE_PRIVATE)
                    val isAllowed=prefs?.getBoolean("feature",false)
                    if(isAllowed as Boolean){
                        Staticated.playNext("PlayNextNormal")
                    }

                }
            }

        }
    }
}
