import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'

/** 建立 STOMP 连接：断线自动重连，间隔 5 秒。 */
export function createRealtimeClient(brokerURL: string, token?: string): Client {
  return new Client({
    brokerURL,
    reconnectDelay: 5000,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {}
  })
}

/** 订阅行程消息主题，返回订阅句柄（调用 unsubscribe 可取消）。 */
export function subscribeTrip(
  client: Client,
  tripId: string,
  onMessage: (message: IMessage) => void
): StompSubscription {
  return client.subscribe(`/topic/trips/${tripId}`, onMessage)
}
