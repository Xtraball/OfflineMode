#import "SBOfflineModeDownloader.h"
#import "RNCachingURLProtocol.h"

@implementation SBOfflineModeDownloader

BOOL running = NO;

- (id)initWithCommandDelegate:(NSObject<CDVCommandDelegate> *)commandDelegate callback:(NSString *)callbackId andURL:(NSURL *)downloadURL {
    if(self = [self init]) {
        callback = callbackId;
        URL = downloadURL;
        cmdDelegate = commandDelegate;
    }
    
    return self;
}

- (void)start {
    if(running) return;
    
    running = YES;
    
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:URL];
    [request setValue:@"true" forHTTPHeaderField:@"X-Native-Cache"];
    
    [[[RNCachingURLProtocol alloc]
      initWithRequest: request
      cachedResponse:nil
      client:self] startLoading];
}

#pragma mark -
#pragma mark Protocol Client for handling manual caching

- (void)URLProtocol:(NSURLProtocol *)protocol
cachedResponseIsValid:(NSCachedURLResponse *)cachedResponse {
    [self sendResult:CDVCommandStatus_OK];
}

- (void)URLProtocol:(NSURLProtocol *)protocol
didCancelAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge {
    [self sendResult:CDVCommandStatus_ERROR];
}

- (void)URLProtocol:(NSURLProtocol *)protocol
   didFailWithError:(NSError *)error {
    [self sendResult:CDVCommandStatus_ERROR];
}

- (void)URLProtocol:(NSURLProtocol *)protocol
        didLoadData:(NSData *)data {
    // No action
}

- (void)URLProtocol:(NSURLProtocol *)protocol
didReceiveAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge {
    // No action
}

- (void)URLProtocol:(NSURLProtocol *)protocol
 didReceiveResponse:(NSURLResponse *)response
 cacheStoragePolicy:(NSURLCacheStoragePolicy)policy {
    [self sendResult:CDVCommandStatus_OK];
}

- (void)URLProtocol:(NSURLProtocol *)protocol
wasRedirectedToRequest:(NSURLRequest *)request
   redirectResponse:(NSURLResponse *)redirectResponse {
    // No action
}

- (void)URLProtocolDidFinishLoading:(NSURLProtocol *)protocol {
    [self sendResult:CDVCommandStatus_OK];
}

#pragma mark -
#pragma mark Utils

BOOL sentResult = NO;

- (void)sendResult:(CDVCommandStatus)status {
    if(sentResult) return;
    [cmdDelegate sendPluginResult:[CDVPluginResult resultWithStatus:status] callbackId:callback];
    sentResult = YES;
}

@end
