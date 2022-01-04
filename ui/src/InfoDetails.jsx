import React from 'react'
import { FadeLoader } from 'react-spinners'
import { Table, Button, ButtonDropdown, DropdownToggle, DropdownMenu, DropdownItem, Card } from 'reactstrap';
import { toast, Slide } from 'react-toastify'
import 'react-toastify/dist/ReactToastify.css'
import {TextColor} from './Styles.jsx'
import { socket, webProtocol } from './App.jsx';

export default class InfoDetails extends React.Component {
    
    constructor() {
        super()
        
        this.state = {
            startSpinner: false,
        }

        this.subscribeForHome = this.subscribeForHome.bind(this)
        this.startSpinner = this.startSpinner.bind(this)
        this.stopSpinnerTimer = this.stopSpinnerTimer.bind(this)
        this.viewIsFocused = this.viewIsFocused.bind(this)
        this.viewIsUnfocused = this.viewIsUnfocused.bind(this)
        this.refreshUserAuthToken = this.refreshUserAuthToken.bind(this)

        this.verifyServerTimestamp = this.verifyServerTimestamp.bind(this)
        this.handleWSOAuthError = this.handleWSOAuthError.bind(this)
        this.handleWSDisconnect = this.handleWSDisconnect.bind(this)
        this.handleWSReconnecting = this.handleWSReconnecting.bind(this)
        this.handleWSReconnectError = this.handleWSReconnectError.bind(this)
        this.handleWSReconnectFailed = this.handleWSReconnectFailed.bind(this)
        this.handleWSReconnect = this.handleWSReconnect.bind(this)
        this.wsRequestStates = this.wsRequestStates.bind(this)
        this.websockStateUpdate = this.websockStateUpdate.bind(this)
        this.startStateRetryTimer = this.startStateRetryTimer.bind(this)
        this.stopStateRetryTimer = this.stopStateRetryTimer.bind(this)
    }

    componentDidMount() {
        // provide context to the browser running this script so the browser
        // can inject javascript to commuicate with us.
        window.app = this

        // don't allow scrolling outside of specific scrollable views.
        document.body.style.overflow = 'hidden'

        // after 500mS of blank screen if the page has not loaded we will start a wiating spinnter
        this.spinnerDelayInterval = setTimeout(this.startSpinner, 500)

        socket.on('stateUpdate', this.websockStateUpdate)
        socket.on('aievent', this.aiEvent)

        socket.on('authError', this.handleWSOAuthError)
        socket.on('disconnect', this.handleWSDisconnect)
        socket.on('reconnecting', this.handleWSReconnecting)
        socket.on('reconnect_error', this.handleWSReconnectError)
        socket.on('reconnect_failed', this.handleWSReconnectFailed)
        socket.on('reconnect', this.handleWSReconnect)

        this.verifyServerTimestamp()

        try {
            // we need to get the OAuth token for the user owned by the Sendal mobile application.
            // the request is sent into the mobile app's execution environment.
            // will result in the mobile app invoking the userAuthTokenUpdate callback function.
            window.ReactNativeWebView.postMessage(JSON.stringify({ event: "UserAuthTokenRequest" }))
        } catch (err) {
            // this is a path that allows debugging to insert a hard-coded for test (or in some cases no) user token
            console.log('The native context does not exist yet for authToken')
            this.subscribeForHome(true)
        }
    }

    componentWillUnmount() {
        document.body.style.overflow = 'unset'

        this.stopSpinnerTimer()
        socket.removeListener('stateUpdate', this.websockStateUpdate)
        socket.removeListener('aievent', this.aiEvent)

        socket.removeListener('authError', this.handleWSOAuthError)
        socket.removeListener('reconnect', this.handleWSDisconnect)
        socket.removeListener('reconnecting', this.handleWSReconnecting)
        socket.removeListener('reconnect_error', this.handleWSReconnectError)
        socket.removeListener('reconnect_failed', this.handleWSReconnectFailed)
        socket.removeListener('reconnect', this.handleWSReconnect)
    }

    userAuthTokenUpdate(receivedToken) {
        // this method is called by the mobile app in response to requests for UserAuthToken
        // The mobile app injects javascript via the window.app definition to run this method.
        this.state.userAuthToken = receivedToken // set immediatly so it's available to the API calls.
        this.setState({ userAuthToken: receivedToken })
        this.subscribeForHome(true)
    }

    verifyServerTimestamp() {
        fetch(webProtocol + document.location.hostname + ":" + document.location.port
            + "/v1/pss/" + this.props.applicationId
            + "/servertimestamp")
            .then(response => {
                if (response.ok == true) {
                    response.json().then(timestamp => {
                        let serverTimestamp = timestamp.serverTimestamp
                        console.log("Comparing timestamp (" + this.state.serverTimestamp + ") to (" + serverTimestamp + ")")

                        if (typeof this.state.serverTimestamp != 'undefined') {
                            if (this.state.serverTimestamp.localeCompare(serverTimestamp) != 0) {
                                console.log("Reloading due to server timestamp (" + this.state.serverTimestamp + ") is different than updated - (" + serverTimestamp + ")")
                                window.location.reload(true)
                            }
                        }

                        this.setState({ serverTimestamp: serverTimestamp })
                    })
                } else {
                    console.log("Error in servertimestamp response - " + response.status)
                }
            })
    }

    refreshUserAuthToken() {
        try {
            window.ReactNativeWebView.postMessage(JSON.stringify({ event: "UserAuthTokenRefreshRequest" }));
        } catch (err) {
            console.log('Unauthorized access - The native context does not exist yet for authToken')
        }
    }

    subscribeForHome(queryStates) {
        // we 'subscribe' to the 3PSS service through our IS server for 
        // the home summary, which drives the information we diplay.
        console.log("Subscribing to home")

        if (typeof window.ReactNativeWebView != 'undefined') {
            window.ReactNativeWebView.postMessage(JSON.stringify({ event: "SetHeaderTitle", headerTitle: 'Light Tracker' }))
        }

        fetch(webProtocol + document.location.hostname + ":" + document.location.port
            + "/v1/pss/" + this.props.applicationId
            + "/users/" + this.props.userId
            + "/homes/" + this.props.homeId
            + "/api/passthrough/usagesummary",
            {
                headers: { authorization: this.state.userAuthToken }
            })
        .then(response => {
            if (response.ok == true) {
                response.json().then(usageSummary => {

                    let dayUsageSummary = [...usageSummary]
                    dayUsageSummary.sort((a, b) => { return (b.usageDaySeconds == a.usageDaySeconds) ? a.deviceName.localeCompare(b.deviceName) : b.usageDaySeconds - a.usageDaySeconds })

                    let weekUsageSummary = [...usageSummary]
                    weekUsageSummary.sort((a, b) => { return (b.usageWeekSeconds == a.usageWeekSeconds) ? a.deviceName.localeCompare(b.deviceName) : b.usageWeekSeconds - a.usageWeekSeconds })

                    let monthUsageSummary = [...usageSummary]
                    monthUsageSummary.sort((a, b) => { return (b.usageMonthSeconds == a.usageMonthSeconds) ? a.deviceName.localeCompare(b.deviceName) : b.usageMonthSeconds - a.usageMonthSeconds })

                    this.setState({
                        dayUsageSummary: dayUsageSummary,
                        weekUsageSummary: weekUsageSummary,
                        monthUsageSummary: monthUsageSummary
                    })
                })
            } else if (response.status == 401 ||
                response.status == 403) {
                console.log("Unauthorized access - " + response.status)
                // in the polled method used for PSS it's likely an app that's been backgrounded has an out of date token, so ask
                // the mobile app for a refresh.
                try {
                    // should be rate as the mobile app should refresh tokens well in advance of their expiration, but
                    // in case something goes wrong or if the app has been running for a long time (i.e. hours)
                    // will result in the mobile app invoking the userAuthTokenUpdate callback function.
                    window.ReactNativeWebView.postMessage(JSON.stringify({ event: "UserAuthTokenRefreshRequest" }))
                } catch (err) {
                    console.log('Unauthorized access - The native context does not exist yet for authToken')
                }
            } else {
                console.log("Failed to retrieve service info - " + response.status)
                toast("Error: Could not get service info, error " + response.status + ".  Please try again by tapping the back button.")
            }
        })
        .catch(error => {
            console.log("Error in retrieving service info - " + error)
            toast("Error: Could not get service info  " + error + ".  Please try again by tapping the back button.")
        })

        // in parallel we check to see if the IS server has updated in case we are on this
        // screen for an extended time (reloading should be a rare event)
        fetch(webProtocol + document.location.hostname + ":" + document.location.port
            + "/v1/pss/" + this.props.applicationId
            + "/servertimestamp")
        .then(response => {
            if (response.ok == true) {
                response.json().then(timestamp => {
                    let serverTimestamp = timestamp.serverTimestamp
                    console.log("Comparing timestamp (" + this.state.serverTimestamp + ") to (" + serverTimestamp + ")")

                    if (typeof this.state.serverTimestamp != 'undefined') {
                        if (this.state.serverTimestamp.localeCompare(serverTimestamp) != 0) {
                            console.log("Reloading due to server timestamp (" + this.state.serverTimestamp + ") is different than updated - (" + serverTimestamp + ")")
                            window.location.reload(true)
                        }
                    }

                    this.setState({ serverTimestamp: serverTimestamp })
                })
            } else {
                console.log("Error in servertimestamp response - " + response.status)
            }
        })

        if (queryStates == true) {
            this.wsRequestStates()
        }
    }

    startStateRetryTimer() {
        this.stopStateRetryTimer()
        this.stateRetryTimer = setTimeout(() => {
            console.log("Retrying state request");
            this.wsRequestStates();
        }, 3000)
    }

    stopStateRetryTimer() {
        if (typeof this.stateRetryTimer != 'undefined') {
            clearTimeout(this.stateRetryTimer)
            this.stateRetryTimer = undefined
        }
    }

    wsRequestStates() {
        this.stopStateRetryTimer()
        // there is a chance for out of sequence states here we will have to address soon

        // state registration is now split, we poll states 
        fetch(webProtocol + document.location.hostname + ":" + document.location.port
            + "/v1/pss/" + this.props.applicationId
            + "/users/" + this.props.userId
            + "/homes/" + this.props.homeId
            + "/pssstateregister",
            {
                headers: {
                    authorization: this.state.userAuthToken,
                    'Content-Type': 'application/json'
                },
                method: 'post',
                body: JSON.stringify({})
            })
            .then(response => {
                if (response.ok == true) {
                    response.json().then(stateUpdates => {
                        this.websockStateUpdate(JSON.stringify(stateUpdates))
                    })
                } else if (response.status == 401 ||
                    response.status == 403) {
                    console.log("Unauthorized access - " + response.status)
                    try {
                        window.ReactNativeWebView.postMessage(JSON.stringify({ event: "UserAuthTokenRefreshRequest" }));
                    } catch (err) {
                        console.log('Unauthorized access - The native context does not exist yet for authToken')
                        this.startStateRetryTimer()
                    }
                } else {
                    console.log("Failed to retrieve states - " + response.status)
                    this.startStateRetryTimer()
                }
            })
            .catch(error => {
                console.log("Error in retrieving states - " + error)
                this.startStateRetryTimer()
            })

        socket.emit('states', {
            homeId: this.props.homeId,
            pssId: this.props.applicationId,
            authorization: this.state.userAuthToken
        })
    }

    handleWSOAuthError() {
        try {
            window.ReactNativeWebView.postMessage(JSON.stringify({ event: "UserAuthTokenRefreshRequest" }))
        } catch (err) {
            console.log('The native context does not exist yet for authToken')
        }
    }

    handleWSDisconnect(reason) {
        console.log("WS disconnect reason - " + reason)
        if (socket.io.connecting.indexOf(socket) === -1) {
            //you should renew token or do another important things before reconnecting
            socket.connect()
        }
    }

    handleWSReconnecting(attemptNumber) {
        console.log("reconnecting attempt - " + attemptNumber)
    }

    handleWSReconnectError(error) {
        console.log("reconnecting error - " + error)
    }

    handleWSReconnectFailed() {
        console.log("reconnecting failed, giving up")
    }

    handleWSReconnect(attemptNumber) {
        console.log("reconnected - " + attemptNumber)
        this.verifyServerTimestamp()

        try {
            window.ReactNativeWebView.postMessage(JSON.stringify({ event: "UserAuthTokenRequest" }));
        } catch (err) {
            console.log('Unauthorized access - The native context does not exist yet for authToken')
            this.subscribeForHome(false)
        }
    }

    websockStateUpdate(stateUpdates) {

        let stateArray = JSON.parse(stateUpdates)

        stateArray.forEach((stateStructMember) => {
            let stateName = stateStructMember.stateIdentifier.stateName;
            let stateValue = stateStructMember.stateValue;

            if (stateName == 'appState') {
                // app state has changed, need to requery the server's information.
                this.getHomeAnomalies(false)
            }
            // else we ignore badge state updates.
        })
    }

    viewIsFocused() {
        // an injected method from the mobile app.  Tells us that the view
        // running this page is now being displayed.  The view may be part
        // of a paretnt tabview or similar which can get de-focused.
        this.subscribeForHome(true)
    }

    viewIsUnfocused() {

    }

    startSpinner() {
        this.setState({ startSpinner: true })
        this.spinnerDelayInterval = undefined
    }

    stopSpinnerTimer() {
        if (typeof this.spinnerDelayInterval != 'undefined') {
            clearTimeout(this.spinnerDelayInterval)
            this.spinnerDelayInterval = undefined
        }
    }

    render() {
        let classes = this.props.classes
        if(typeof this.state.dayUsageSummary != 'undefined') {
            this.stopSpinnerTimer()

            console.log(webProtocol + document.location.hostname + ":" + document.location.port
            + "/v1/pss/" + this.props.applicationId
            + "/users/"+this.props.userId+"/homes/"+this.props.homeId+"/assets/index.html")
            return(
                <div>
                    <p style={{textAlign: 'center'}}>Light Tracker Details</p>
                    <Button style={{ position: 'absolute', bottom: 0, marginLeft:'20%', marginRight:'20%', width: '60%'}} onClick={() => window.ReactNativeWebView.postMessage(JSON.stringify({ event: "PopoverWebNavigation", 
                        targetURL: webProtocol + document.location.hostname + ":" + document.location.port
                            + "/v1/pss/" + this.props.applicationId
                            + "/users/"+this.props.userId+"/homes/"+this.props.homeId+"/assets/index.html"}))}>
                        <p>Full page Screen</p>
                    </Button>
                </div>
            )
        } else {
            return (<div className={classes.LoadingSpinner}>
                <FadeLoader
                    sizeUnit={"px"}
                    size={20}
                    color={TextColor}
                    loading={this.state.startSpinner} />
            </div>)
        }
    }
}
