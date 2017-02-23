package com.lynbrookrobotics.potassium.dashboard

import com.lynbrookrobotics.potassium.Signal

import scala.reflect.ClassTag

trait DashboardContext {
  def addDataset[T: ClassTag](name: String, dataset: Signal[T]): Unit
}
