package com.malinskiy.marathon.apple.cmd.remote.ssh.sshj

import com.malinskiy.marathon.apple.cmd.remote.ssh.sshj.auth.SshAuthentication
import com.malinskiy.marathon.apple.cmd.remote.ssh.sshj.config.PerformanceDefaultConfig
import com.malinskiy.marathon.apple.sshj.PatchedSSHClient
import com.malinskiy.marathon.execution.withRetrySync
import com.malinskiy.marathon.log.MarathonLogging
import kotlinx.coroutines.channels.Channel
import net.schmizz.sshj.transport.TransportException
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.time.Duration

/**
private val knownHostsPath: File? = null,
 *
 */
class SshjCommandExecutorFactory(
    private val connectionAttempts: Int = 4,
    private val keepAliveInterval: Duration = Duration.ofSeconds(60L),
    private val channelCapacity: Int = Channel.BUFFERED,
) {

    private val logger = MarathonLogging.logger {}

    /**
     * @throws TransportException
     * @throws ConnectException
     * @throws IOException
     */
    fun connect(
        addr: String,
        port: Int,
        authentication: SshAuthentication,
        hostKeyVerifier: HostKeyVerifier = PromiscuousVerifier(),
        debug: Boolean,
    ): SshjCommandExecutor {
        val client = PatchedSSHClient(PerformanceDefaultConfig(verbose = debug)).apply {
            connection.keepAlive.keepAliveInterval = keepAliveInterval.toSeconds().toInt()
            connection.keepAlive.name = "$addr:$port keep-alive"
            addHostKeyVerifier(hostKeyVerifier)
        }
        withRetrySync(connectionAttempts, 0, logger) {
            client.connect(addr, port)
        }
        authentication.authenticate(client)
        return SshjCommandExecutor(
            SshjCommandHost(
                addr,
                port,
                authentication,
                client.remoteCharset
            ), client, channelCapacity
        )
    }
}
