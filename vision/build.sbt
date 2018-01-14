resolvers += "WPILib-Maven" at "http://team846.github.io/wpilib-maven"
resolvers += "opencv-maven" at "https://github.com/WPIRoboticsProjects/opencv-maven/raw/mvn-repo"
// TODO: replace w/ OpenCV's Maven repo?
// waiting on https://github.com/opencv/opencv/issues/4588

libraryDependencies += "edu.wpi.first" % "cscore" % "2018.1.1"
libraryDependencies += "org.opencv" % "opencv-java" % "3.1.0"

libraryDependencies += "edu.wpi.first" % "ntcore" % "2018.1.1"
