package com.yueki1993.bloom.filtree;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FooTest {
    @Test
    public void test() {
        assertThat(new Foo().foo(), is(1));
    }
}