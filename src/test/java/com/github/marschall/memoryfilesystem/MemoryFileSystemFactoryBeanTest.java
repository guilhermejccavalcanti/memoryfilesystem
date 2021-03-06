package com.github.marschall.memoryfilesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MemoryFileSystemFactoryBeanTest {

  @Autowired
  private FileSystem fileSystem;

  @Test
  public void isOpen() {
    assertTrue(this.fileSystem.isOpen());
    assertEquals("memory:test:///", this.fileSystem.getPath("").toUri().toString());
  }

}
