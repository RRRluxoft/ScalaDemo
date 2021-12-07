package home.work.onboarding.orderservice.order

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.option._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class OrderRepositorySpec extends AsyncWordSpec with Matchers {
  import OrderRepositorySpec._

  private def mapRepositoryInstance() = await(OrderRepository.mapInstance[TestEffect])
  private def refRepositoryInstance() = await(OrderRepository.refInstance[TestEffect])

  testOrderRepositoryImpl(mapRepositoryInstance)
  testOrderRepositoryImpl(refRepositoryInstance)

  private def testOrderRepositoryImpl(createRepositoryInstance: () => OrderRepository[TestEffect]): Unit =
    s"Order repository implementation: ${createRepositoryInstance.getClass.getName}" should {
      "create some order" in {
        val repository = createRepositoryInstance()
        await(repository.allOrders) shouldBe List.empty
        noException shouldBe thrownBy {
          await(repository.saveOrder(someOrder, someOrderLines))
        }
        await(repository.allOrders) shouldBe List(someOrder)
      }
      "fail to create another order of the same ID" in {
        val repository = createRepositoryInstance()
        await(repository.saveOrder(someOrder, someOrderLines))
        assertThrows[OrderAlreadyExists] {
          await(repository.saveOrder(someOrder, someOrderLines))
        }
      }
      "list orders" in {
        val repository = createRepositoryInstance()
        await(repository.saveOrder(someOrder, someOrderLines))
        await(repository.allOrders) shouldBe List(someOrder)
      }
      "list lines" in {
        val repository = createRepositoryInstance()
        await(repository.saveOrder(someOrder, someOrderLines))
        await(repository.allLines) shouldBe someOrderLines.toList
      }
      "set status" in {
        val repository = createRepositoryInstance()
        val expectedStatus = Order.Status.Picking
        await(repository.saveOrder(someOrder.copy(status = Order.Status.Waiting), someOrderLines))
        await(repository.setStatus(someOrder.id, expectedStatus))
        await(repository.allOrders).find(_.id == someOrder.id).map(_.status) shouldBe expectedStatus.some
        val nonExistingId = Order.Id("FAKE")
        noException shouldBe thrownBy {
          await(repository.setStatus(nonExistingId, expectedStatus))
        }
      }
    }
}

object OrderRepositorySpec {
  type TestEffect[A] = IO[A]

  private def await[A](effect: TestEffect[A]): A = effect.unsafeRunSync()
  private val someOrderId = Order.Id("123")
  private val someOrder = Order(someOrderId, Order.Status.Waiting)

  private val someSkuId = Sku.Id("321321123")
  private val someOtherSkuId = Sku.Id("89878672")
  private val someOrderLineId = LineId("1")
  private val someOtherOrderLineId = LineId("2")

  private val someOrderLines = NonEmptyList.of(
    OrderLine(someOrderLineId, someOrder.id, picked = false, someSkuId),
    OrderLine(someOtherOrderLineId, someOrder.id, picked = false, someOtherSkuId)
  )

}
