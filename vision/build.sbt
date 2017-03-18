resolvers += "WPILib-Maven" at "http://team846.github.io/wpilib-maven"
resolvers += "opencv-maven" at "https://github.com/WPIRoboticsProjects/opencv-maven/raw/mvn-repo"
// TODO: replace w/ OpenCV's Maven repo?
// waiting on https://github.com/opencv/opencv/issues/4588

libraryDependencies += "edu.wpi.first" % "cscore" % "2017.3.1"
libraryDependencies += "org.opencv" % "opencv-java" % "3.1.0"
