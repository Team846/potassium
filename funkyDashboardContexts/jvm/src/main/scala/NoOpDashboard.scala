import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.dashboard.DashboardContext

import scala.reflect.ClassTag

object NoOpDashboard extends DashboardContext {
  override def addDataset[T: ClassTag](name: String, dataset: Signal[T]): Unit = {}
}
