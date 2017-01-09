#import "SBOfflineModeManager.h"
#import "TMReachability.h"

@implementation SBOfflineModeManager

@synthesize useCache, isOnline, checkConnectionURL;

BOOL isAwareOfReachability = NO;
BOOL postNotifications = NO;
NSTimer *checkTimer;

#pragma mark Singleton Methods

+ (id)sharedManager {
    static MyManager *sharedMyManager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedMyManager = [[self alloc] init];
    });
    return sharedMyManager;
}

- (id)init {
    if (self = [super init]) {
        checkConnectionURL = nil;
        useCache = YES;
        isOnline = YES;
        
        // Allocate a reachability object
        TMReachability* reach = [Reachability reachabilityWithHostname:@"www.google.com"];
        
        reach.reachableBlock = ^(Reachability*reach)
        {
            [self checkConnection];
        };
        
        reach.unreachableBlock = ^(Reachability*reach)
        {
            [self checkConnection];
        };
        
        [reach startNotifier];
        
    }
    return self;
}

- (void)dealloc {
    // Should never be called, but just here for clarity really.
}

#pragma mark Actual Code

- (void)watchReachability {
    postNotifications = YES;
    [self postNotification];
}

@private

- (void)setUnreachable {
    isAwareOfReachability = YES;
    isOnline = NO;
    useCache = YES;
    
    [self postNotification];
}

- (void)setReachable {
    isAwareOfReachability = YES;
    isOnline = YES;
    useCache = NO;
    
    [self postNotification];
}

- (void)postNotification {
    if(isAwareOfReachability && postNotifications) {
        [[NSNotificationCenter defaultCenter]
         postNotificationName:SBOfflineModeManagerConnectionStatusChanged
         object:self];
    }
}

- (void)checkConnection {
    NSURLSession *session = [NSURLSession sharedSession];
    [[session dataTaskWithURL:[NSURL URLWithString:self.checkConnectionURL]
            completionHandler:^(NSData *data,
                                NSURLResponse *response,
                                NSError *error) {
                NSString *response = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];

                if([response isEqualToString:@"ok"]) {
                    [self setReachable];
                } else {
                    [self setUnreachable];
                }
                
                [self startTimer];
            }] resume];
}

- (void)startTimer {
    
    if([checkTimer isKindOfClass: [NSTimer class]]) {
        [checkTimer invalidate];
        checkTimer = nil;
    }
    
    checkTimer = [NSTimer
                  scheduledTimerWithTimeInterval:3
                  repeats:NO
                  block:^(NSTimer *timer) {
                      [self checkConnection]
                  }];
    
}

@end
