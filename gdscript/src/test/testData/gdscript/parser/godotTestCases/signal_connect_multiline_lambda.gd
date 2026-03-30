func test():
    # Multiline lambda as first arg, followed by trailing arg
    store.connect(func(x):
        do_something(x)
    , CONNECT_ONE_SHOT)

    # Multiline lambda as only arg
    call(func():
        print("hello")
    )

    # Single-line lambda with trailing comma arg (should already work)
    store.connect(func(x): do_something(x), CONNECT_ONE_SHOT)

    # Real-world: multiline lambda with if-body on separate line
    store.scenario_list_ready.connect(
        func(scenarios: Array) -> void:
            if scenarios.size() > 0:
                store.load_scenario(scenarios[0].get("id", "")),
        CONNECT_ONE_SHOT,
    )

    # Single-line lambda inside connect
    store.store_ready.connect(func() -> void: store.list_scenarios(), CONNECT_ONE_SHOT)
