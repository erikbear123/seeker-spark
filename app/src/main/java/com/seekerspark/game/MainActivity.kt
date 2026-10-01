package com.seekerspark.game

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.solana.mobilewalletadapter.clientlib.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val bg = Color.rgb(11, 15, 28)
    private val white = Color.rgb(239, 245, 255)
    private val muted = Color.rgb(149, 163, 190)
    private val mint = Color.rgb(66, 237, 192)
    private val prefs by lazy { getSharedPreferences("spark", MODE_PRIVATE) }
    private lateinit var sender: ActivityResultSender
    private lateinit var wallet: MobileWalletAdapter
    private lateinit var root: LinearLayout
    private var address: String? = null
    private var board: SparkBoard? = null
    private var screen = "home"
    private var busy = false
    private var resultScore = 0
    private var resultHits = 0
    private var roundId = ""
    private var completedAt = ""
    private var proofJson: String? = null
    private var scoreText: TextView? = null
    private var timeText: TextView? = null
    private var lifeText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sender = ActivityResultSender(this)
        wallet = MobileWalletAdapter(ConnectionIdentity(
            Uri.parse("https://github.com/erikbear123/seeker-spark"), Uri.parse("/favicon.ico"), "Seeker Spark"
        )).apply { blockchain = Solana.Mainnet }
        // A fresh wallet authorization is required after an app restart.
        // Authorization tokens are never written to disk or included in backups.
        if (savedInstanceState?.getString("screen") == "result") {
            resultScore = savedInstanceState.getInt("score")
            resultHits = savedInstanceState.getInt("hits")
            roundId = savedInstanceState.getString("round") ?: UUID.randomUUID().toString()
            completedAt = savedInstanceState.getString("completed") ?: Instant.now().toString()
            proofJson = savedInstanceState.getString("proof")
            showResult()
        } else showHome()
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("screen", screen); out.putInt("score", resultScore); out.putInt("hits", resultHits)
        out.putString("round", roundId); out.putString("completed", completedAt); out.putString("proof", proofJson)
    }
    override fun onResume() { super.onResume(); if (screen == "game") board?.resumeGame() }
    override fun onPause() { board?.pauseGame(); super.onPause() }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 16) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun text(value: String, size: Float, color: Int = white, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); includeFontPadding = false
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    private fun gap(height: Int) { root.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun button(label: String, primary: Boolean = true, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 15f; isAllCaps = false
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        setTextColor(if (primary) bg else white)
        background = shape(if (primary) mint else Color.rgb(26, 35, 55))
        stateListAnimator = null; minHeight = dp(52)
        contentDescription = label
        setOnClickListener { if (!busy) action() }
    }
    private fun addButton(label: String, primary: Boolean = true, action: () -> Unit) {
        root.addView(button(label, primary, action), LinearLayout.LayoutParams(-1, dp(52)))
    }
    private fun reset(page: String) {
        board?.pauseGame(); board = null; screen = page
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(dp(24), dp(18), dp(24), dp(16))
        }
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(24) + bars.left, dp(18) + bars.top, dp(24) + bars.right, dp(16) + bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
    private fun eyebrow(value: String) { root.addView(text(value, 11f, Color.rgb(170, 149, 255), true).apply { letterSpacing = 0.16f }) }
    private fun status(): String = if (address != null) "${address!!.take(5)}…${address!!.takeLast(5)}" else "Guest · wallet optional"

    private fun showHome() {
        reset("home")
        eyebrow("30 SECOND ARCADE")
        gap(10); root.addView(text("Seeker Spark", 36f, white, true))
        gap(8); root.addView(text("Catch the light. Beat your best.", 15f, muted))
        gap(22)
        addButton(if (busy) "Opening wallet…" else if (address == null) "Connect Seeker wallet" else "Wallet connected · ${status()}", false) {
            if (address == null) connectWallet() else walletMenu()
        }
        gap(12)
        val best = prefs.getInt(bestKey(), 0)
        root.addView(text("PERSONAL BEST   $best", 12f, mint, true))
        gap(12)
        board = SparkBoard(this)
        root.addView(board, LinearLayout.LayoutParams(-1, 0, 1f))
        gap(12)
        root.addView(text("Tap green sparks. Avoid red traps.\nBuild a streak for bonus points.", 14f, muted).apply { gravity = Gravity.CENTER; setLineSpacing(dp(3).toFloat(), 1f) })
        gap(18); addButton("Play 30-second round") { startGame() }
        gap(12)
        root.addView(text("$bestLabel · signed scores stay on your phone", 10f, muted).apply { gravity = Gravity.CENTER })
    }
    private val bestLabel: String get() = if (address == null) "Guest mode" else "Wallet linked"
    private fun bestKey() = "best:${address ?: "guest"}"

    private fun startGame() {
        proofJson = null; roundId = UUID.randomUUID().toString()
        reset("game"); window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        eyebrow("SEEKER SPARK")
        gap(14)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        scoreText = text("0", 38f, white, true)
        timeText = text("30s", 24f, mint, true).apply { gravity = Gravity.END }
        row.addView(scoreText, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(timeText)
        root.addView(row)
        gap(8); lifeText = text("3 lives · Streak 0", 13f, muted); root.addView(lifeText)
        gap(12)
        val engine = GameEngine()
        board = SparkBoard(this, engine).apply {
            onStats = {
                scoreText?.text = engine.score.toString()
                timeText?.text = "${(engine.remainingMs + 999) / 1000}s"
                lifeText?.text = "${engine.lives} lives · Streak ${engine.combo}"
            }
            onFinish = {
                resultScore = engine.score; resultHits = engine.hits; completedAt = Instant.now().toString()
                val old = prefs.getInt(bestKey(), 0)
                if (resultScore > old) prefs.edit().putInt(bestKey(), resultScore).apply()
                showResult()
            }
        }
        root.addView(board, LinearLayout.LayoutParams(-1, 0, 1f))
        gap(14); root.addView(text("Green + points    /    Red − life", 13f, muted).apply { gravity = Gravity.CENTER })
        gap(14); addButton("End round", false) {
            board?.pauseGame()
            AlertDialog.Builder(this).setTitle("End this round?").setMessage("This unfinished round will not be saved.")
                .setPositiveButton("End round") { _, _ -> showHome() }
                .setNegativeButton("Keep playing") { _, _ -> board?.resumeGame() }
                .setOnCancelListener { board?.resumeGame() }.show()
        }
        board?.resumeGame()
    }

    private fun showResult() {
        reset("result")
        eyebrow("ROUND COMPLETE")
        gap(16); root.addView(text("Nice seeking.", 31f, white, true))
        gap(12); root.addView(text("$resultScore", 72f, mint, true))
        root.addView(text("POINTS  ·  $resultHits sparks caught", 13f, muted))
        gap(22)
        root.addView(text("PERSONAL BEST   ${prefs.getInt(bestKey(), 0)}", 12f, white, true))
        gap(22)
        val signed = proofJson != null
        root.addView(text(if (signed) "✓ Wallet signature verified" else "Make this round yours", 20f, if (signed) mint else white, true))
        gap(10)
        root.addView(text(if (signed) "Your wallet signed this score. The signature was checked on this phone."
            else "Sign your score with Seeker's wallet. A message signature has no network fee.", 14f, muted).apply { setLineSpacing(dp(4).toFloat(), 1f) })
        gap(16)
        addButton(if (busy) "Check your wallet…" else if (signed) "Copy signed score proof" else "Sign score with wallet", false) {
            if (signed) {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Seeker Spark score proof", proofJson))
                Toast.makeText(this, "Signed proof copied", Toast.LENGTH_SHORT).show()
            } else signScore()
        }
        gap(10)
        root.addView(text("${status()}\nLocal score proof · no public leaderboard", 11f, muted))
        root.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        addButton("Play again") { startGame() }
        gap(10); addButton("Back to home", false) { showHome() }
        gap(12)
    }

    private fun walletMenu() {
        AlertDialog.Builder(this).setTitle("Connected wallet").setMessage(address)
            .setPositiveButton("Disconnect") { _, _ -> disconnectWallet() }.setNegativeButton("Close", null).show()
    }
    private fun redraw() { if (screen == "result") showResult() else if (screen == "home") showHome() }
    private fun walletError(message: String) {
        AlertDialog.Builder(this).setTitle("Wallet request did not finish")
            .setMessage(message).setPositiveButton("OK", null).show()
    }
    private fun connectWallet() {
        busy = true; redraw()
        lifecycleScope.launch {
            try {
                when (val result = wallet.connect(sender)) {
                    is TransactionResult.Success -> {
                        val key = result.authResult.accounts.firstOrNull()?.publicKey
                        require(key?.size == 32) { "Wallet did not return a valid Solana account." }
                        address = ScoreProof.base58(key!!)
                        Toast.makeText(this@MainActivity, "Wallet connected", Toast.LENGTH_SHORT).show()
                    }
                    is TransactionResult.NoWalletFound -> walletError("Open Seed Vault Wallet and finish its setup, then try again.")
                    is TransactionResult.Failure -> walletError("Request was declined, cancelled, or timed out. You can try again.\n${result.message}")
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { walletError(e.message ?: "Please try connecting again.") }
            finally { busy = false; redraw() }
        }
    }
    private fun disconnectWallet() {
        busy = true; redraw()
        lifecycleScope.launch {
            try {
                when (wallet.disconnect(sender)) {
                    is TransactionResult.Success -> { address = null; proofJson = null }
                    else -> walletError("Disconnect could not be confirmed. Please try again, or revoke access inside your wallet.")
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { walletError(e.message ?: "Could not disconnect.") }
            finally { busy = false; redraw() }
        }
    }

    private fun signScore() {
        if (roundId.isEmpty() || screen != "result") return
        busy = true; redraw()
        lifecycleScope.launch {
            try {
                val result = wallet.transact(sender) { auth ->
                    val key = auth.accounts.firstOrNull()?.publicKey ?: error("No wallet account returned")
                    require(key.size == 32)
                    val walletAddress = ScoreProof.base58(key)
                    val message = ScoreProof.message(roundId, resultScore, resultHits, walletAddress, completedAt)
                    val signed = signMessagesDetached(arrayOf(message.toByteArray(Charsets.UTF_8)), arrayOf(key)).messages.firstOrNull()
                        ?: error("Wallet did not return a signed message")
                    require(signed.message.contentEquals(message.toByteArray(Charsets.UTF_8))) { "Wallet returned a different message" }
                    val signature = signed.signatures.firstOrNull() ?: error("Missing signature")
                    require(ScoreProof.verify(key, signed.message, signature)) { "Wallet signature verification failed" }
                    JSONObject().put("app", "Seeker Spark").put("version", 1).put("roundId", roundId)
                        .put("score", resultScore).put("sparks", resultHits).put("completedAt", completedAt)
                        .put("wallet", walletAddress).put("message", message)
                        .put("signature", ScoreProof.base58(signature)).put("signatureEncoding", "base58")
                        .put("scope", "local-wallet-signed-score").toString(2)
                }
                when (result) {
                    is TransactionResult.Success -> {
                        proofJson = result.payload
                        address = ScoreProof.base58(result.authResult.accounts.first().publicKey)
                        prefs.edit().putString("lastProof", proofJson).putInt(bestKey(), maxOf(resultScore, prefs.getInt(bestKey(), 0))).apply()
                        Toast.makeText(this@MainActivity, "Signature verified", Toast.LENGTH_SHORT).show()
                    }
                    is TransactionResult.NoWalletFound -> walletError("No compatible wallet found. Please open Seed Vault Wallet and finish setup.")
                    is TransactionResult.Failure -> walletError("The score was not signed. You can retry.\n${result.message}")
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { walletError(e.message ?: "The signature could not be verified.") }
            finally { busy = false; redraw() }
        }
    }
}
