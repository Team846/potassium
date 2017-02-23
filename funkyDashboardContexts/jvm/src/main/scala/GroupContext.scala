import com.lynbrookrobotics.funkydashboard.FunkyDashboard
import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.dashboard.DashboardContext

import scala.reflect.ClassTag

class GroupContext(groupName: String, dashboard: FunkyDashboard) extends DashboardContext {
  override def addDataset[T: ClassTag](name: String, dataset: Signal[T]): Unit = {
  }
}
