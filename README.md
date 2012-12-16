Memory File System [![Build Status](https://travis-ci.org/marschall/memoryfilesystem.png?branch=master)](https://travis-ci.org/marschall/memoryfilesystem)
==================
An in memory implementation of a [JSR-203](http://jcp.org/en/jsr/detail?id=203) (Java 7) file system for testing purposes.

```xml
<dependency>
    <groupId>com.github.marschall</groupId>
    <artifactId>memoryfilesystem</artifactId>
    <version>0.1.0</version>
</dependency>
```


Supported
---------
* <code>SeekableByteChannel</code>
* <code>FileChannel</code>
* <code>AsynchronousFileChannel</code>
* <code>InputStream</code>
* <code>OutputStream</code>
* <code>BasicFileAttributeView</code>, <code>BasicFileAttributes</code>
* <code>DosFileAttributeView</code>, <code>DosFileAttributes</code>
* <code>PosixFileAttributeView</code>, <code>PosixFileAttributes</code>
* <code>UserDefinedFileAttributeView</code>
* <code>FileLock</code>
* <code>PathMatcher</code>
  * glob
  * regex
* <code>StandardCopyOption</code>
  * REPLACE_EXISTING
  * COPY_ATTRIBUTES
  * ATOMIC_MOVE
* <code>StandardOpenOption</code>
  * READ
  * WRITE
  * TRUNCATE_EXISTING
  * CREATE
  * DELETE_ON_CLOSE
* symbolic links
* symbolic link loop detection

Not Supported
-------------
* <code>FileChannel#map</code>, <code>MappedByteBuffer</code> has final methods that call native methods
* <code>WatchService</code>
* <code>FileTypeDetector</code>
* faked DOS attribute view under Linux, totally unspecified
* <code>UnixFileAttributeView</code>, [sun package](http://www.oracle.com/technetwork/java/faq-sun-packages-142232.html), totally unspecified
* any meaningful access checks
* files larger than 16MB
* <code>StandardOpenOption</code>
  * SPARSE
  * SYNC
  * DSYNC
* hard links

FAQ
---
### Does it have bugs?
Quite likely.

### What license is it?
MIT

### Does it support concurrent access?
Yes, but hasn't been subject much scrutiny so bugs are likely. 

### Does it work with the zipfs provider?
No, see http://bugs.sun.com/view_bug.do?bug_id=8004789

### Is it production ready?
No, it's only intended for testing purposes.

### Does it scale?
No

### Does it have any dependencies?
No

### Does it work with Spring?
Yes, there is a POJO factory bean. It has been tested with Spring 3.1.3 but since it doesn't have any dependencies on Spring it should work with every ⩾ 2.x version. You can of course also use Java configuration or any other IoC container.

### Does it work with OSGi?
Yes, it's a bundle and there's an activator that prevents class loader leaks. You should use the `MemoryFileSystemBuilder` instead of `FileSystems#newFileSystem` because `ServiceLoader` uses the thread context class loader. `MemoryFileSystemBuilder` avoids this by passing in the correct class loader.

### Does it do any logging?
No

### But I want all my file access logged
A logging file system that wraps an other file system is the best way to do this.

Usage
-----
### Getting Started
The easiest way to get started is to use the `MemoryFileSystemBuilder`

```java
try (FileSystem fileSystem = MemoryFileSystemBuilder.newEmpty().build("test")) {
  Path p = fileSystem.getPath("p");
  System.out.println(Files.exists(p));
}
```

It's important to know that at any given time there can only be one memory file system with a given name. Any attempt to create a memory file system with the name of an existing one will throw an exception. 

There are other `new` methods on `MemoryFileSystemBuilder` that allow you to create different file systems and other methods that allow you to customize the file system.

### Next Steps
You probably want to create a JUnit `TestRule` that sets up and tears down a file system for you. A rule can look like this

```java
final class FileSystemRule implements TestRule {

  private FileSystem fileSystem;

  FileSystem getFileSystem() {
    return this.fileSystem;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        FileSystemRule.this.fileSystem = MemoryFileSystemBuilder.newEmpty().build("name");
        try {
          base.evaluate();
        } finally {
          FileSystemRule.this.fileSystem.close();
        }
      }

    };
  }

}
```

and is used like this
```java
public class FileSystemTest {

  @Rule
  public final FileSystemRule rule = new FileSystemRule();

  @Test
  public void lockAsyncChannel() throws IOException {
    FileSystem fileSystem = this.rule.getFileSystem();

    Path path = fileSystem.getPath("sample.txt");
    assertFalse(Files.exists(path));
  }

}
```

It's important to note that the field holding the rule must be public. If you're using an IoC container for integration tests check out the section below.

### Spring
The `com.github.marschall.memoryfilesystem.MemoryFileSystemFactoryBean` provides integration with Spring.

```xml
  <bean id="memoryFileSystemFactory"
      class="com.github.marschall.memoryfilesystem.MemoryFileSystemFactoryBean">
    <property name="name" value="test" />
  </bean>

  <bean id="memoryFileSystem" destroy-method="close"
    factory-bean="memoryFileSystemFactory" factory-method="getObject" />
```

You can of course also write a [Java Configuration](http://static.springsource.org/spring/docs/3.0.x/spring-framework-reference/html/beans.html#beans-java) class and a `@Bean` method that uses `MemoryFileSystemBuilder` to create a new file system. Or a CDI class with a `@Produces` method that uses `MemoryFileSystemBuilder` to create a new file system. 


Guidelines for Testable File Code
================================

The following guidelines are designed to help you write code that can easily be tested using this project. In general code using the old `File` API has to moved over to the new Java 7 API.

* Inject a `Path` or `FileSystem` instance into the object doing the file handling. This allows you to pass in an instance of a memory file system when testing and an instance of the default file system when running in production. You can always the the file system of a path by using `Path#getFileSystem()`.
* Don't use `File`, `FileInputStream`, `FileOutputStream` and `RandomAccessFile`. These classes are hard wired to the default file system.
  * Use `Path` instead of `File`.
  * Use `SeekableByteChannel` instead of `RandomAccessFile`. Use `Files#newByteChannel` to create an instance of `SeekableByteChannel`.
  * Use `Files#newInputStream` and `Files#newOutputStream` to create `InputStream`s and `OutputStream`s on files.
  * Use `FileChannel#open` instead of `FileInputStream#getChannel()`, `FileOutputStream#getChannel()`, or `RandomAccessFile#getChannel()` to create a ``FileChannel`
* Use `FileSystem#getPath(String, String...)` instead of `Paths#get` to create a `Path` instance because the latter creates an instance on the default file system.
