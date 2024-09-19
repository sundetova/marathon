package com.malinskiy.marathon.io

import com.google.common.io.Files
import com.malinskiy.marathon.device.DeviceFeature
import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.NetworkState
import com.malinskiy.marathon.device.OperatingSystem
import com.malinskiy.marathon.extension.escape
import com.malinskiy.marathon.test.toTestName
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

class FileManagerTest {
    private val output = Files.createTempDir()

    private companion object {
        val poolId = DevicePoolId("testPoolId")
        val deviceInfo = DeviceInfo(
            operatingSystem = OperatingSystem("23"),
            serialNumber = "xxyyzz",
            model = "Android SDK built for x86",
            manufacturer = "unknown",
            networkState = NetworkState.CONNECTED,
            deviceFeatures = listOf(DeviceFeature.SCREENSHOT, DeviceFeature.VIDEO),
            healthy = true
        )
        val shortNameTest = com.malinskiy.marathon.test.Test(
            "com.example",
            "Clazz",
            "method",
            emptyList()
        )

        val longNameTest = com.malinskiy.marathon.test.Test(
            "com.example",
            "Clazz",
            "pefalgkxbnfrvxsdprtoprggsibqaeobgiyvaiatysajemcoamubppvppibyuknyeqhipkvzwyesngazgycpkwzahyocsfsxclfgaxvpwrsrjukwrtlwpreewlvekqvkibvtwlizaxwwvzbgfepwypiwpbfgzchjrytvzawoulupuitbxkgchjihbbjdacohxleojtcMyRandomTextEndingWithMoreThan256CharactersLengthToVerifyCutPoint",
            emptyList()
        )

        val longNamedParameterizedTest = com.malinskiy.marathon.test.Test(
            "com.example",
            "Clazz",
            "someRidiculousLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongNamepefalgkxbnfrvxsdprtoprggsibqaeobgiyvaiatysajemcoamubppvppibyuknyeqhipkvzwyesngazgycpkwzah[Some parameter with spaces: /rest/api/2/endpoint, Other parameter: 1, And another one: {3}]",
            emptyList()
        )

        val batchId = "batchId"
    }

    @Test
    fun createFilenameNormalLengthTest() {
        val fileManager = FileManager(0, 255, output)
        val file = fileManager.createFile(FileType.LOG, poolId, deviceInfo, shortNameTest, batchId)
        file.name shouldBeEqualTo "com.example.Clazz#method-batchId.log"
    }

    @Test
    fun createFilenameLongLengthMethodLimitedPathTest() {
        val fileManager = FileManager(255, 0, output)
        val file = fileManager.createFile(FileType.LOG, poolId, deviceInfo, longNameTest, batchId)
        file.absolutePath.length shouldBeEqualTo 255
        val filenameLimit = 255 - file.parentFile.absolutePath.length - File.separator.length
        file.name shouldBeEqualTo "${longNameTest.toTestName()}-${batchId}.log".escape().take(filenameLimit)
    }

    @Test
    fun testCreateFilenameNamedParameterizedLong() {
        val fileManager = FileManager(255, 0, output)
        val file = fileManager.createFile(FileType.LOG, poolId, deviceInfo, longNamedParameterizedTest, batchId)
        file.absolutePath.length shouldBeEqualTo 255
        val filenameLimit = 255 - file.parentFile.absolutePath.length - File.separator.length
        file.name shouldBeEqualTo "${longNamedParameterizedTest.toTestName()}-${batchId}.log".escape().take(filenameLimit)
    }

    @Test
    fun testDeviceSerialEscaping() {
        val fileManager = FileManager(0, 255, output)
        val file = fileManager.createFile(
            FileType.LOG, poolId, DeviceInfo(
                operatingSystem = OperatingSystem("23"),
                serialNumber = "127.0.0.1:5037:emulator-5554",
                model = "Android SDK built for x86",
                manufacturer = "unknown",
                networkState = NetworkState.CONNECTED,
                deviceFeatures = listOf(DeviceFeature.SCREENSHOT, DeviceFeature.VIDEO),
                healthy = true
            )
        )
        file.name shouldBeEqualTo "127.0.0.1-5037-emulator-5554.log"
    }

    @Test
    fun testScreenshotfile() {
        val fileManager = FileManager(0, 255, output)
        val file = fileManager.createFile(
            FileType.SCREENSHOT_PNG, poolId, DeviceInfo(
                operatingSystem = OperatingSystem("23"),
                serialNumber = "127.0.0.1:5037:emulator-5554",
                model = "Android SDK built for x86",
                manufacturer = "unknown",
                networkState = NetworkState.CONNECTED,
                deviceFeatures = listOf(DeviceFeature.SCREENSHOT, DeviceFeature.VIDEO),
                healthy = true
            ),
            test = shortNameTest,
            id = "on-device-test",
        )
        file.name shouldBeEqualTo "com.example.Clazz#method-on-device-test.png"
    }

    @Test
    fun testTooLongOutputPathUnlimitedFilename() {
        val test = com.malinskiy.marathon.test.Test(
            pkg = "com.xxyyzzxxyy.android.abcdefgh.abcdefghi",
            clazz = "PackageNameTest",
            method = "useAppContext",
            metaProperties = emptyList()
        )

        val tempDir = Files.createTempDir()
        val proposedPath = Paths.get(tempDir.absolutePath, FileType.LOG.name, poolId.name, deviceInfo.safeSerialNumber)
        val limitedMaxPath = 255
        val additionalPathCharacters = limitedMaxPath - proposedPath.absolutePathString().length
        val limitedOutputDirectory = File(tempDir, "x".repeat(additionalPathCharacters))
        val limitedFileManager = FileManager(limitedMaxPath, 0, limitedOutputDirectory)
        val file = limitedFileManager.createFile(FileType.LOG, poolId, deviceInfo, test, batchId)

        file.path.length shouldBeEqualTo 255
    }
}
