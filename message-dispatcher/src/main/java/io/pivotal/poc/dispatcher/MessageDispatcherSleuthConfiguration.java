package io.pivotal.poc.dispatcher;

import java.util.ArrayList;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanExtractor;
import org.springframework.cloud.sleuth.SpanInjector;
import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.cloud.stream.binding.CompositeMessageChannelConfigurer;
import org.springframework.cloud.stream.binding.MessageChannelConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Marius Bogoevici
 */
@Configuration
public class MessageDispatcherSleuthConfiguration {

    @Bean
    public Sampler defaultSampler() {
        return new AlwaysSampler();
    }

    @Bean
    public static BeanPostProcessor postProcessMessageChannelConfigurer(@Lazy SpanInjector<MessageBuilder<?>> spanInjector,
                                                                        @Lazy SpanExtractor<Message<?>> spanExtractor,
                                                                        @Lazy Tracer tracer,
                                                                        @Lazy SpanReporter spanReporter) {

        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
                // Work around https://github.com/spring-cloud/spring-cloud-stream/issues/705
                if (o instanceof CompositeMessageChannelConfigurer) {
                    ArrayList<MessageChannelConfigurer> configurers = new ArrayList<>();
                    configurers.add((MessageChannelConfigurer) o);
                    configurers.add(sleuthChannelConfigurer(spanInjector, spanExtractor, tracer, spanReporter));
                    return new CompositeMessageChannelConfigurer(configurers);
                }
                return o;
            }

            @Override
            public Object postProcessAfterInitialization(Object o, String s) throws BeansException {
                return o;
            }
        };
    }

    @Bean
    public static MessageChannelConfigurer sleuthChannelConfigurer(@Lazy SpanInjector<MessageBuilder<?>> spanInjector,
                                                                   @Lazy SpanExtractor<Message<?>> spanExtractor,
                                                                   @Lazy Tracer tracer,
                                                                   @Lazy SpanReporter spanReporter) {
        return new MessageChannelConfigurer() {
            @Override
            public void configureInputChannel(MessageChannel messageChannel, String channelName) {
            }

            @Override
            public void configureOutputChannel(MessageChannel messageChannel, String channelName) {
                if (messageChannel instanceof ChannelInterceptorAware && !"sleuth".equals(channelName)) {
                    ((ChannelInterceptorAware) messageChannel).addInterceptor(new ChannelInterceptorAdapter() {
                        @Override
                        public Message<?> preSend(Message<?> message, MessageChannel channel) {
                            MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
                            Span streamSpan = tracer.createSpan("stream");
                            streamSpan.logEvent("stream.start");

                            Span brokerSpan = tracer.createSpan("broker", streamSpan);
                            brokerSpan.logEvent("broker.sent");
                            spanInjector.inject(brokerSpan, builder);
                            builder.setHeader("parent-span-id", streamSpan.getSpanId());
                            builder.setHeader("parent-trace-id", streamSpan.getTraceId());
                            builder.setHeader("parent-span-name", streamSpan.getName());
                            tracer.close(brokerSpan);
                            spanReporter.report(brokerSpan);
                            tracer.close(streamSpan);
                            spanReporter.report(streamSpan);
                            return builder.build();
                        }
                    });
                }
            }
        };
    }
}
