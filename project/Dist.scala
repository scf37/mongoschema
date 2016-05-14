import java.io.StringWriter
import java.util.Properties

import sbt.Keys._
import sbt._

import scala.util.Try

object Dist {
  lazy val assemble = taskKey[Seq[File]]("Assemble application at target/dist")
  lazy val incrementVersion = taskKey[Unit]("Increment application version")
  lazy val dist = taskKey[Seq[File]]("Build versioned application distribution")
//  lazy val writeBuildProperties = taskKey[File]("Write build.properties file to resources")

  lazy val settings = Seq(
    assemble <<= assembleTask,
    dist <<= distTask,
    resourceGenerators in Compile <+= writeBuildPropertiesTask
  )

  private[this] lazy val distTask = Def.sequential(incrementVersionTask, Def.task {
    val r = assembleTask.value
    val log = streams.value.log
    log.info("Release complete, distribution is available at " + (target.value / "dist"))
    r
  })




  private[this] lazy val writeBuildPropertiesTask = Def.task[Seq[File]] {
    val props = Map(
    "name" -> name.value,
    "version" -> version.value,
    "build_revision" -> ("git rev-parse HEAD"!!).trim,
    "scm_repository" -> Try("git config --get remote.origin.url"!!).getOrElse("<no remote>").trim,
    "build_last_few_commits" -> (Seq("git", "log", "-n", "5", "--pretty=%h %ad %an %s")!!))

    val propString = {
      val p = new Properties()
      props.foreach { case (k, v) =>
        p.setProperty(k, v)
      }
      val sw = new StringWriter()
      p.store(sw, "")
      sw.toString
    }

    val file = resourceManaged.value / "build.properties"

    IO.write(file, propString.getBytes("UTF-8"))

    Seq(file)
  }

  private[this] lazy val assembleTask = Def.task[Seq[File]] {

    val copyOutpath = target.value / "dist"

    IO.delete(copyOutpath)

    val libs: List[File] = fullClasspath.all(ScopeFilter( inAnyProject, inConfigurations(Runtime) )).value
      .flatten.map(_.data).filter(_.isFile).toList.distinct
    val modules: Seq[File] = packageBin.all(ScopeFilter( inAnyProject, inConfigurations(Runtime) )).value

    val jars: List[File] = libs ++ modules

    val copy: List[(File, File)]  = jars.map(f => (f, copyOutpath / "lib" / f.getName))
    val log = streams.value.log

    log.info(s"Copying to ${copyOutpath}:")
    log.info(s"  ${copy.map(_._1).mkString("\n")}")
    IO.copy(copy)
    log.info("copy web:" + (baseDirectory.value / "web", copyOutpath))
    IO.copyDirectory(baseDirectory.value / "web", copyOutpath / "web")

    val startupScript = IO.read(baseDirectory.value / "project/app")
      .replaceAll("@MainClass@", "me.scf37.ms.Main")
      .replaceAll("@ExtraJvmArguments@", "-Xmx512m -XX:+UseG1GC  -Djava.net.preferIPv4Stack=true")

    IO.write(copyOutpath / "bin/app", startupScript)
    (copyOutpath / "bin/app").setExecutable(true)

    jars :+ (copyOutpath / "web") :+ (copyOutpath / "bin/app")
  }


  private[this] lazy val incrementVersionTask = Def.task[File] {
    val log = streams.value.log

    if (("git diff-index --quiet HEAD --"!) != 0) {
      log.error("There are uncommited files")
      throw new IllegalStateException("There are uncommited files")
    }
    



    val nextVersion = (version.value.toInt + 1).toString
    val versionFile = baseDirectory.value / "version.sbt"
    val q = "\""
    IO.write(versionFile, IO.read(versionFile).replace(s"$q${version.value}$q", "\"" + nextVersion + "\""))

    log.info(s"git add $versionFile")
    s"git add $versionFile"!!

    log.info(s"git commit -m ${q}[ci skip]Releasing version ${version.value}$q")
    Seq("git", "commit", "-m", s"${q}[ci skip]Releasing version ${version.value}$q")!!

    versionFile
  }

}