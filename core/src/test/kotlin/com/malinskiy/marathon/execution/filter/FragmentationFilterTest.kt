package com.malinskiy.marathon.execution.filter

import com.malinskiy.marathon.exceptions.ConfigurationException
import com.malinskiy.marathon.generateTests
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.malinskiy.marathon.test.Test as MarathonTest

class FragmentationFilterTest {
    @Test
    fun testFragmentsProperly() {
        val filter0 = FragmentationFilter(0, 3)
        val filter1 = FragmentationFilter(1, 3)
        val filter2 = FragmentationFilter(2, 3)
        
        filter0.validate()
        filter1.validate()
        filter2.validate()

        val tests: List<MarathonTest> = generateTests(15)

        val fragment0 = filter0.filter(tests)
        val fragment1 = filter1.filter(tests)
        val fragment2 = filter2.filter(tests)
        
        (fragment0 + fragment1 + fragment2).size shouldBeEqualTo 15
    }

    @Test
    fun testFilterNot() {
        val filter0 = FragmentationFilter(0, 3)
        val filter1 = FragmentationFilter(1, 3)
        val filter2 = FragmentationFilter(2, 3)

        filter0.validate()
        filter1.validate()
        filter2.validate()

        val tests: List<MarathonTest> = generateTests(15)

        val fragment0 = filter0.filterNot(tests)
        val fragment1 = filter1.filter(tests)
        val fragment2 = filter2.filter(tests)
        
        fragment0 shouldContainSame fragment1 + fragment2
    }
    
    @Test
    fun testThrowsException() {
        assertThrows<ConfigurationException> { FragmentationFilter(-1, 3).validate() }
        assertThrows<ConfigurationException> { FragmentationFilter(1, -1).validate() }
        assertThrows<ConfigurationException> { FragmentationFilter(5, 2).validate() }
    }
}
