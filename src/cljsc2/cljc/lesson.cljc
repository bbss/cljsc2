(ns cljsc2.cljc.lesson)

(def lesson
  {:db/id 1
   :lesson/steps
   [{:db/id 9
     :step/description "Go through the steps of this interactive tutorial to learn how to build a bot!"
     :step/title "Start coding a StarCraft II bot"}
    {:step/description "Let's use some functions to create a plan for building a base. How would we do such a thing?",
     :step/title "Setting up a base",
     :step/explanation "In order to set up a base we need to execute plans, this runs a plan on a connection to a starcraft II game.
Running a plan means we can respond to the starcraft environment: analyze the data (e.g. what are your units (doing), any visible enemy units etc.) and in response to that send actions to play the game."
     :step/init-value "",
     :step/last-time 1532523731602,
     :step/wrap-dom-node :code-cell,
     :db/id "#fulcro/tempid[\"c5447873-c22a-464d-9d85-5d4d58c68d53\"]",
     :step/changes
     [{:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"ee6a93ee-0f50-4bf8-ad8c-72f4393fe025\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"bcf0f590-61d0-4939-8306-91e6a2e596e2\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"c5163be0-5802-4dc1-8db2-593ef7186134\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"60319644-a342-4b81-bad4-782f87746fe7\"]"}}]}
      {:editor-change/dt 1120,
       :db/id "#fulcro/tempid[\"04884d35-f4ab-47b7-b224-d1cf632030a3\"]",
       :editor-change/change
       {:editor-text-change/text ["()"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"948a6b4c-db98-4691-b97e-0df0a364bee6\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 0,
         :db/id
         "#fulcro/tempid[\"043e0f99-2603-406b-8714-973bf43f1c1b\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 0,
         :db/id
         "#fulcro/tempid[\"beae6c2f-9324-468f-a366-d8f9c6d410fb\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"fcf8567b-c482-4378-a1b6-9216d428b1d5\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"422cfa67-178b-4f61-916e-2e1879f0dd7c\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 1,
          :db/id
          "#fulcro/tempid[\"144e7d72-64a0-4d31-952e-dfd6c12a52de\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 1,
          :db/id
          "#fulcro/tempid[\"daf0ef3e-538e-41cf-b5bc-ac3ad249e208\"]"}}]}
      {:editor-change/dt 1047,
       :db/id "#fulcro/tempid[\"54cd7755-7b3c-4175-b5c0-d3c9170b2120\"]",
       :editor-change/change
       {:editor-text-change/text ["e"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"1bafe8ad-64bd-4f11-8cd7-dd9ed1156432\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 1,
         :db/id
         "#fulcro/tempid[\"72fac3fb-9933-4c69-8af6-f4a7bfdac165\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 1,
         :db/id
         "#fulcro/tempid[\"bfc0f5f0-b4f4-44e5-8f36-7a534aa9b513\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"19e733bf-9d7a-466e-8731-a9dd3661a77a\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"d78a9f03-e38d-4903-8828-b7a0287d4238\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 2,
          :db/id
          "#fulcro/tempid[\"6eb88686-f002-4374-bedb-e50859ca225e\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 2,
          :db/id
          "#fulcro/tempid[\"5895fdf4-249f-4af4-8152-ccffb826bf9d\"]"}}]}
      {:editor-change/dt 111,
       :db/id "#fulcro/tempid[\"98820d97-3155-4aca-a7a3-e28e1df6d4b4\"]",
       :editor-change/change
       {:editor-text-change/text ["x"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"a1c23907-0310-453f-82bd-61a461b66775\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 2,
         :db/id
         "#fulcro/tempid[\"a3ca2d2d-bfbf-422c-862f-722fe4732d7e\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 2,
         :db/id
         "#fulcro/tempid[\"4ce9c873-c010-4574-ab3e-95da842235c5\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"1fbcc9b4-1089-43ec-8906-3411facee5d7\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"69b4dece-dda5-420d-9a58-105fe35380d3\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 3,
          :db/id
          "#fulcro/tempid[\"891d8b2b-72a7-4557-b130-bf73e3d90ddd\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 3,
          :db/id
          "#fulcro/tempid[\"b66c937e-44d9-4016-89e7-90993a06125f\"]"}}]}
      {:editor-change/dt 87,
       :db/id "#fulcro/tempid[\"92f7e15d-3006-41f6-9cc5-200b78e78b94\"]",
       :editor-change/change
       {:editor-text-change/text ["e"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"183a3800-3510-4054-a0fe-d0f839463a88\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 3,
         :db/id
         "#fulcro/tempid[\"5d98c17a-7469-4bc5-a7be-f8da5cd69fc8\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 3,
         :db/id
         "#fulcro/tempid[\"92bdbe36-ef3d-4d82-a139-dd2e0e1bebcf\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"7f40bd61-2836-47a6-a31b-086d888a6d9d\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"8e39f9e6-3122-4c09-b783-348504294d3a\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"a4a5b2d1-09a3-4b89-9b37-f6d7e4c90b28\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"5a2825e0-524c-4123-a5f7-1cf2bea998ec\"]"}}]}
      {:editor-change/dt 114,
       :db/id "#fulcro/tempid[\"1cfe0fd6-25e8-467e-b913-191a292adac0\"]",
       :editor-change/change
       {:editor-text-change/text ["c"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"fdb6cfc3-6014-4d5b-afbf-f36f8d5e2728\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 4,
         :db/id
         "#fulcro/tempid[\"5812aca4-c1e6-463a-963f-03b26bdc98bd\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 4,
         :db/id
         "#fulcro/tempid[\"3f6bb8c8-ed0a-4ca2-9761-d599c8393293\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"d7fc4afe-62f7-4250-9630-346e957accc8\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"2642a4e8-de9b-4e01-bf4a-a9c9e33a0efc\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 5,
          :db/id
          "#fulcro/tempid[\"82b3c52d-1e58-464b-8b00-f51c738b248a\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 5,
          :db/id
          "#fulcro/tempid[\"e6f369c0-de7a-4fc9-a70e-7e474ca6095c\"]"}}]}
      {:editor-change/dt 112,
       :db/id "#fulcro/tempid[\"0041f24c-f578-48d3-9317-26cccd4d6a28\"]",
       :editor-change/change
       {:editor-text-change/text ["u"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"7cf4d5ba-c679-41d5-b568-2e059f6280d5\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 5,
         :db/id
         "#fulcro/tempid[\"a93c5af4-a1b8-482c-b845-7d396ac73646\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 5,
         :db/id
         "#fulcro/tempid[\"89e3c80d-4aa7-4b83-9edf-bffd3184a266\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"9b1993a7-6cce-47e7-8901-c93734d6a5d0\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"d4877e6a-206a-419c-9872-ed2c8bc51d21\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 6,
          :db/id
          "#fulcro/tempid[\"89ce1c5f-3c65-4ee7-b01f-785a41742fbf\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 6,
          :db/id
          "#fulcro/tempid[\"d73260ca-90cc-412d-8ee1-e54b25dd134d\"]"}}]}
      {:editor-change/dt 86,
       :db/id "#fulcro/tempid[\"b5623d5b-dc5a-4559-b36d-4c5b49123996\"]",
       :editor-change/change
       {:editor-text-change/text ["t"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"5d98c168-a121-4cf3-81a8-531b76d3715c\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 6,
         :db/id
         "#fulcro/tempid[\"70de54aa-3f47-47b9-b9e5-54513ac36c65\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 6,
         :db/id
         "#fulcro/tempid[\"3930c70d-de90-428a-8f80-0ee8120449cb\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"9a9628a3-d8f1-4c1a-a308-e6894e2f5ec1\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"0d865306-7c87-40f2-a775-05524e777536\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 7,
          :db/id
          "#fulcro/tempid[\"d366c4c8-a0fe-4b05-9320-e6c271c99320\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 7,
          :db/id
          "#fulcro/tempid[\"668b6138-108a-4d91-9c59-c7c5da5f10f3\"]"}}]}
      {:editor-change/dt 69,
       :db/id "#fulcro/tempid[\"cece3e73-2c92-4f3f-8aff-dc7fe288f44a\"]",
       :editor-change/change
       {:editor-text-change/text ["e"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"441a1225-e811-4cdf-af23-73e24728ca47\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 7,
         :db/id
         "#fulcro/tempid[\"75ca68de-0559-4a34-b564-d150e2b8ba6e\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 7,
         :db/id
         "#fulcro/tempid[\"6b6bbb64-357d-48f9-8862-8ee256743b53\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"fbe8e201-f557-4424-85c4-39e907a72131\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"288a811e-b8fa-4c57-bd0b-5433c9fd384a\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 8,
          :db/id
          "#fulcro/tempid[\"4a8ab214-af42-4097-ba52-ec678d676077\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 8,
          :db/id
          "#fulcro/tempid[\"99a77ec9-4cc7-4f4b-bccd-699fe8af1929\"]"}}]}
      {:editor-change/dt 227,
       :db/id "#fulcro/tempid[\"d42ec5f1-6a00-4455-a962-21c8b7a6266d\"]",
       :editor-change/change
       {:editor-text-change/text ["-"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"08af7d69-bad9-4928-b30f-e43726943f25\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 8,
         :db/id
         "#fulcro/tempid[\"32bb8721-7dce-4303-85b6-ecc7ea7a2fdb\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 8,
         :db/id
         "#fulcro/tempid[\"200d9072-d17e-4597-b054-9b82cdb690fa\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"541c971b-cdd3-414f-ba90-2c9cf11b8fe7\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"64f522c4-a5ef-450c-a4d2-74e34c77c778\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 9,
          :db/id
          "#fulcro/tempid[\"027c85c1-599d-4748-b1c2-a9f032112428\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 9,
          :db/id
          "#fulcro/tempid[\"882e5a4e-972e-48bb-8198-3094b6703162\"]"}}]}
      {:editor-change/dt 164,
       :db/id "#fulcro/tempid[\"473fb747-966e-4d46-8277-a06ebac2d8c6\"]",
       :editor-change/change
       {:editor-text-change/text ["p"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"f42e4382-760c-4f43-bf7a-aeb02c49781d\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 9,
         :db/id
         "#fulcro/tempid[\"cfe37520-004d-4396-bfec-9bf3b544eeeb\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 9,
         :db/id
         "#fulcro/tempid[\"cb34e34c-8b8b-44f8-9489-6e39bcabfe3b\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"5d56639e-c37f-4a05-aa41-4c39bf52af1b\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"09b17726-4701-48d4-99fc-b122ea8d9bca\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 10,
          :db/id
          "#fulcro/tempid[\"2cfc7f7f-9cc9-4ee1-982e-6fb9d78e25de\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 10,
          :db/id
          "#fulcro/tempid[\"e06d20e5-31f4-48bf-94bb-8d0b2e8a12bd\"]"}}]}
      {:editor-change/dt 69,
       :db/id "#fulcro/tempid[\"5c6ef023-78a5-4a4f-b38e-59cb98e8eccb\"]",
       :editor-change/change
       {:editor-text-change/text ["l"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"fe397d5a-0593-42e4-95b7-b87c15e22e81\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 10,
         :db/id
         "#fulcro/tempid[\"952cacfb-aae0-464e-a1b8-b14941ca9e97\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 10,
         :db/id
         "#fulcro/tempid[\"dacc1488-e675-44ed-955e-de6335f751b3\"]"}}}
      {:editor-change/dt 2,
       :db/id "#fulcro/tempid[\"5dc75338-c11d-481f-ab55-419fa58d0a8e\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"5ce567fc-89f2-46da-a5b1-cf58f818ba89\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 11,
          :db/id
          "#fulcro/tempid[\"96355fe1-7bd9-43b8-97bd-a2e4c2b1bf95\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 11,
          :db/id
          "#fulcro/tempid[\"b6cf3942-0aa7-485d-abdf-5077dd11d592\"]"}}]}
      {:editor-change/dt 92,
       :db/id "#fulcro/tempid[\"88a37676-7800-41f6-b88d-70f4da581f93\"]",
       :editor-change/change
       {:editor-text-change/text ["a"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"9fd79ba8-5613-4133-8892-d6e620482542\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 11,
         :db/id
         "#fulcro/tempid[\"69c0eb72-1f47-4a7f-ac2f-96ab5903e67e\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 11,
         :db/id
         "#fulcro/tempid[\"7ae19f7d-1dfe-4147-8e1c-36bcdbf166c4\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"e5a586d9-bea9-40a3-83a8-b19a6b6d587f\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"976a0d42-bf6b-485e-ac66-ec260e7813f9\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 12,
          :db/id
          "#fulcro/tempid[\"5b8a3e8c-f456-4541-8988-62c84fc4868f\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 12,
          :db/id
          "#fulcro/tempid[\"2c5fe897-4bdf-4d05-a23d-b80a7b9c0afc\"]"}}]}
      {:editor-change/dt 68,
       :db/id "#fulcro/tempid[\"498c4eb7-6bf0-441a-8c2a-73b57427b03b\"]",
       :editor-change/change
       {:editor-text-change/text ["n"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"b0905e1b-e21c-4304-99b2-d9ec6f932ecc\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 12,
         :db/id
         "#fulcro/tempid[\"bcb271cc-8959-4ed8-ada5-fa7ba4e9a780\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 12,
         :db/id
         "#fulcro/tempid[\"4ad3a0dd-23fb-48ce-b443-3ffd571bdb1d\"]"}}}
      {:editor-change/dt 2,
       :db/id "#fulcro/tempid[\"324fc158-be50-4f02-9710-83d861273656\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"ecbdadae-7ac4-4f97-98bf-1d833d27604f\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 13,
          :db/id
          "#fulcro/tempid[\"5ee70ec9-2031-422d-9d61-7de2c2ac8799\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 13,
          :db/id
          "#fulcro/tempid[\"0637cf40-2264-4c67-928e-2f4ee8072fc1\"]"}}]}
      {:editor-change/dt 104,
       :db/id "#fulcro/tempid[\"cb87fe9e-91b8-49ff-834a-10a70cf178da\"]",
       :editor-change/change
       {:editor-text-change/text ["s"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"87e704f5-1217-4275-b298-95eb7b9956d8\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 13,
         :db/id
         "#fulcro/tempid[\"e3007972-2f57-48f8-9361-755804ea8ea8\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 13,
         :db/id
         "#fulcro/tempid[\"4f47d5b1-ab4c-459f-a826-42be7cafd6fb\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"bc8c8f4d-95c7-4d83-b76f-395b17a25682\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"1a9bfded-8d92-41a5-b1e9-42593c59d86d\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 14,
          :db/id
          "#fulcro/tempid[\"99b6b3d7-8d46-4acd-9816-da62c03d45e9\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 14,
          :db/id
          "#fulcro/tempid[\"17e532cc-7666-4955-8e3c-7ad42237390a\"]"}}]}
      {:editor-change/dt 578,
       :db/id "#fulcro/tempid[\"cf24531a-15f6-442e-9174-f3e31cec0058\"]",
       :editor-change/change
       {:editor-text-change/text ["" ""],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"7d2f0a90-0ac4-4b96-9f6c-37f0814a23c3\"]",
        :editor-text-change/from
        {:editor-caret/line 0,
         :editor-caret/ch 14,
         :db/id
         "#fulcro/tempid[\"976e8cae-cd65-4b80-b310-e2c718003aee\"]"},
        :editor-text-change/to
        {:editor-caret/line 0,
         :editor-caret/ch 14,
         :db/id
         "#fulcro/tempid[\"0d59ee0b-3dd9-4bac-b1bd-a5117a5e9722\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"26cd2ad3-a219-48af-a76f-b18c6bafdc8b\"]",
       :editor-change/change
       {:editor-text-change/text ["    "],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"c9640365-186d-4768-a191-53e19e8bf1ae\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 0,
         :db/id
         "#fulcro/tempid[\"aaa203be-ed7e-4af6-b999-15bcbf88e7f8\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 0,
         :db/id
         "#fulcro/tempid[\"1f27e786-4e4f-4a4a-bd77-5f42b9edb743\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"de5c6e9e-fd73-4a70-8f15-860df2846ab0\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"543a1656-bc7d-4752-97e7-0e66bc354fa0\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"6d842596-1100-4922-a1da-47597a3786d2\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"d5d1ca0a-2083-4d34-b678-0744bdefcb9e\"]"}}]}
      {:editor-change/dt 6,
       :db/id "#fulcro/tempid[\"b454adc2-7fcb-4c66-937c-ac9a59f85a3c\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"3b5b6b0c-ca9b-4621-943b-b93330fe4287\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"2a6fe533-b8a3-406a-b507-7fe53ffb240b\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"e7c4d45f-bbce-459e-b37f-44d5311309c8\"]"}}]}
      {:editor-change/dt 5,
       :db/id "#fulcro/tempid[\"5326a6fb-fdb3-47c5-9826-57c26299ff95\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"0b498b81-dd25-4aec-92b8-1987f127e407\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"cc1e172d-2920-4856-a0a0-980882c8ab39\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"72940ab0-21f3-4209-b7ce-1d76f2c39621\"]"}}]}
      {:editor-change/dt 205,
       :db/id "#fulcro/tempid[\"6563c0fb-c4e8-4f4d-850c-7faa0d37740a\"]",
       :editor-change/change
       {:editor-text-change/text ["()"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"2f64725c-e3b9-4f95-9d56-0f29f8add8cd\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 4,
         :db/id
         "#fulcro/tempid[\"0e01a66e-f483-4e8e-9072-7690c4e1137e\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 4,
         :db/id
         "#fulcro/tempid[\"54c12ab5-836f-4915-9e55-c05eabdaade7\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"71bd4f0f-48c1-4431-8cdd-786df390f011\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"51eb72c6-3d9c-4a17-8578-14ce6b4cb0b7\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 5,
          :db/id
          "#fulcro/tempid[\"8fbb4792-4e6c-419a-8a08-d6986ce2cc06\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 5,
          :db/id
          "#fulcro/tempid[\"69449418-6a69-421f-9ed8-302d605ac0ea\"]"}}]}
      {:editor-change/dt 4,
       :db/id "#fulcro/tempid[\"775290c4-f80b-40c8-b596-67537a256404\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"11c48855-63d2-4928-90f4-4d0fe0dd5eff\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"68a37563-a474-44e7-ac27-8ec6921418e4\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"6fc34028-ce86-4e3f-a454-5a14dc006f89\"]"}}]}
      {:editor-change/dt 4,
       :db/id "#fulcro/tempid[\"ac7f6fee-f9b1-47a3-86eb-52b31e5c301e\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"eaa53696-a633-4cde-9f84-b00b40278ff7\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 5,
          :db/id
          "#fulcro/tempid[\"084843b6-bb1e-4385-ab0a-098704b7259d\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 5,
          :db/id
          "#fulcro/tempid[\"c79d0038-3a8d-404b-8028-665e5204b498\"]"}}]}
      {:editor-change/dt 348,
       :db/id "#fulcro/tempid[\"5fbb6ada-f97a-4e40-bbe7-cec7366f42ec\"]",
       :editor-change/change
       {:editor-text-change/text ["b"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"a4b7a04e-f392-4ccf-aab9-25e0572b5549\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 5,
         :db/id
         "#fulcro/tempid[\"98bdf7af-8aab-4df9-a30a-4837aa715aa1\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 5,
         :db/id
         "#fulcro/tempid[\"dc383da3-cb3c-4cec-879f-53fe84a32dab\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"af0cce4b-c337-45ec-b82e-587c6fcbd14e\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"34626856-e057-4cc3-86e6-7d74825f8f86\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 6,
          :db/id
          "#fulcro/tempid[\"895e8e43-de05-4041-8e3b-f07ebf386d1d\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 6,
          :db/id
          "#fulcro/tempid[\"50ea8892-bcad-4dd0-a2eb-5d5672bd5297\"]"}}]}
      {:editor-change/dt 79,
       :db/id "#fulcro/tempid[\"453189ff-2d07-4fcd-a855-f667adc0cdbf\"]",
       :editor-change/change
       {:editor-text-change/text ["u"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"21c1ec22-688b-4d01-bf59-4cb73767004b\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 6,
         :db/id
         "#fulcro/tempid[\"ae968df1-ed4d-4392-82fb-fa24b2eda43a\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 6,
         :db/id
         "#fulcro/tempid[\"92910df6-178b-45f6-9766-b892b477664b\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"8e691a45-fea9-47bc-b802-2409a86039a4\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"b8c4859f-531e-49a5-82ac-818750c2ecf9\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 7,
          :db/id
          "#fulcro/tempid[\"d59cb8ee-0579-4273-878a-9d949040653b\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 7,
          :db/id
          "#fulcro/tempid[\"178fbbfc-1c4c-4ff0-ad80-c6dbe0ddb1f7\"]"}}]}
      {:editor-change/dt 14,
       :db/id "#fulcro/tempid[\"25217986-db3b-48b6-8864-80108cf6f8a6\"]",
       :editor-change/change
       {:editor-text-change/text ["i"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"d9f53848-14de-4adc-a7dc-e5729163011d\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 7,
         :db/id
         "#fulcro/tempid[\"388d3c02-79f7-478a-9a07-31602174359c\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 7,
         :db/id
         "#fulcro/tempid[\"a8060259-b1cb-4f2e-a85b-e83369ba3038\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"553e7255-f7ff-4cac-a9fe-49d6fcbb2e0c\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"66a20784-0906-436b-982a-216b28ee580c\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 8,
          :db/id
          "#fulcro/tempid[\"982db03b-fef1-4b99-85c8-0a85463a3892\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 8,
          :db/id
          "#fulcro/tempid[\"4ebc1757-0b35-492d-b234-990239d2cac9\"]"}}]}
      {:editor-change/dt 114,
       :db/id "#fulcro/tempid[\"6a7ef368-4a26-47ee-818a-8d72ad4f3c54\"]",
       :editor-change/change
       {:editor-text-change/text ["l"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"16719a69-4fef-467f-b920-f63fe2308823\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 8,
         :db/id
         "#fulcro/tempid[\"9859eee2-3850-4e8e-aef1-30dae3139650\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 8,
         :db/id
         "#fulcro/tempid[\"de6335cf-d89a-499b-8f09-b4f91ecffd34\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"e536fa6a-9981-4b82-904c-8bf7913aa8c9\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"d4becf41-e7a2-4fc9-a041-868adf6f1005\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 9,
          :db/id
          "#fulcro/tempid[\"3946422d-a372-4a5a-b598-9e1210526b81\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 9,
          :db/id
          "#fulcro/tempid[\"df9df5c4-d085-40ee-9d92-58c1847b8ea5\"]"}}]}
      {:editor-change/dt 139,
       :db/id "#fulcro/tempid[\"f5926a10-8da4-4b7b-b2d7-ddc1ecb89972\"]",
       :editor-change/change
       {:editor-text-change/text ["d"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"e54ffe1e-11fb-4178-91b6-57a0ebdba4e6\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 9,
         :db/id
         "#fulcro/tempid[\"cead4b47-cf9f-423d-b8d0-2a1ec3cbb713\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 9,
         :db/id
         "#fulcro/tempid[\"646e5a70-4839-4753-a3e3-16ad75514e6a\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"16126694-db7b-4ba4-b9e9-e7368137ad0a\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"c81ed58b-6130-416b-9f27-41cfaa2d77fd\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 10,
          :db/id
          "#fulcro/tempid[\"274809eb-491f-4d1a-8f58-0f3cacae722c\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 10,
          :db/id
          "#fulcro/tempid[\"8e5a8cf3-905e-47be-8351-d9937b4dce8d\"]"}}]}
      {:editor-change/dt 250,
       :db/id "#fulcro/tempid[\"69fbf1eb-0418-49f3-aeee-8775973aacbd\"]",
       :editor-change/change
       {:editor-text-change/text [" "],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"dbc0d500-bfc0-44c9-8031-3f814ba9cf73\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 10,
         :db/id
         "#fulcro/tempid[\"f0a42f99-7a02-4228-90cb-246ddcf2555e\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 10,
         :db/id
         "#fulcro/tempid[\"bae9da32-4d1d-4289-86ab-1ebc537f6460\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"5639ec6f-e7ca-4393-b1bf-f53bb1014875\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"1f07b183-216b-4b69-a451-97f95dc05e47\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 11,
          :db/id
          "#fulcro/tempid[\"be05e667-0006-4fe5-820d-9b4a325de39f\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 11,
          :db/id
          "#fulcro/tempid[\"0bed3635-625e-4282-909e-1229bf3ce5ee\"]"}}]}
      {:editor-change/dt 261,
       :db/id "#fulcro/tempid[\"4d10b2e5-457a-49ad-9746-0231d9fbe739\"]",
       :editor-change/change
       {:editor-text-change/text ["\"\""],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"e8dd6c33-50f2-4365-b685-4a32af503403\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 11,
         :db/id
         "#fulcro/tempid[\"0cf48e43-20d7-4c4a-8a28-d6fa08ba8aab\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 11,
         :db/id
         "#fulcro/tempid[\"b63ae3cf-f392-4749-a900-956cc7fdb35f\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"472a3ca9-6f67-457d-b6b3-ea49863fd7df\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"e22539ef-48bf-4744-90cf-740b2349dff5\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 12,
          :db/id
          "#fulcro/tempid[\"0d73d3b5-9600-463f-b2f3-6933571f6521\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 12,
          :db/id
          "#fulcro/tempid[\"38357b35-4a3a-44c9-b7d1-605ca7268c77\"]"}}]}
      {:editor-change/dt 225,
       :db/id "#fulcro/tempid[\"b1209438-1d5e-42ba-8ce2-34558ca03d6f\"]",
       :editor-change/change
       {:editor-text-change/text ["S"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"9fca1703-888a-425a-b92a-f4e249d601a6\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 12,
         :db/id
         "#fulcro/tempid[\"da5ad3e1-d856-4372-9312-ae4ed7960af2\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 12,
         :db/id
         "#fulcro/tempid[\"ff732c78-03bc-4db3-829b-4c1ba2bbdb47\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"b8c0f228-7230-4ac7-8cd7-a3dda3c8c941\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"25793282-180e-497f-9995-59de8d53cd50\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 13,
          :db/id
          "#fulcro/tempid[\"3a9cd03e-8b6d-4e38-9e01-9e38260e3ced\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 13,
          :db/id
          "#fulcro/tempid[\"fc0a612a-69bd-4dda-8850-095be7f51e89\"]"}}]}
      {:editor-change/dt 212,
       :db/id "#fulcro/tempid[\"d04ddfb3-3905-4776-b295-64767a5cc9f6\"]",
       :editor-change/change
       {:editor-text-change/text ["u"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"2005e4a2-179e-4cf2-825e-fa04283eabfc\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 13,
         :db/id
         "#fulcro/tempid[\"bcca66a9-c49b-432e-a1b3-cec54c2e6467\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 13,
         :db/id
         "#fulcro/tempid[\"03ef2095-7e96-46dd-9a3d-39fbf96fc180\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"1153828d-ec14-45a1-aacd-afb9a778ea14\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"6cd50ba0-452b-4733-9e52-226e20ab02a0\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 14,
          :db/id
          "#fulcro/tempid[\"a3a12fda-513d-4a32-8aa9-7fa91b16582f\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 14,
          :db/id
          "#fulcro/tempid[\"aad97681-67ed-4fa0-a145-fec38c0fa929\"]"}}]}
      {:editor-change/dt 148,
       :db/id "#fulcro/tempid[\"0813fb81-94aa-47b9-84f1-476185b962ed\"]",
       :editor-change/change
       {:editor-text-change/text ["p"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"8398baf8-75ba-4ac7-ae17-f1b237c95e48\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 14,
         :db/id
         "#fulcro/tempid[\"aebfa471-cdbf-4dea-b6e3-32e0d55d6d9d\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 14,
         :db/id
         "#fulcro/tempid[\"16e1f600-4ec0-49e6-8ee1-2092e8046a6e\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"eb50d80b-8007-482a-b0f5-ffa849e419d0\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"cd9ef58d-66e3-4cd3-8070-0f1e343494ba\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 15,
          :db/id
          "#fulcro/tempid[\"4b0c275d-4077-44a3-940e-cbea39768471\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 15,
          :db/id
          "#fulcro/tempid[\"f69d908d-4a34-4da5-94df-0172810f00d8\"]"}}]}
      {:editor-change/dt 108,
       :db/id "#fulcro/tempid[\"c21436c4-740f-4c9d-abc1-3630f723af85\"]",
       :editor-change/change
       {:editor-text-change/text ["p"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"479c6d33-ab25-4690-9ad5-9ab1d1004d26\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 15,
         :db/id
         "#fulcro/tempid[\"f6adb44e-7a8b-4cb6-ae10-67afb0bec661\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 15,
         :db/id
         "#fulcro/tempid[\"d3b3f950-b28c-4cf8-bc6e-bc11be85f0c1\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"32bef9fc-2405-4ac7-b122-19e78261016a\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"2266d690-2d19-47ae-bac1-2c8af9a76cf0\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 16,
          :db/id
          "#fulcro/tempid[\"d8884e3e-c602-4dd9-9f82-30c424a8975f\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 16,
          :db/id
          "#fulcro/tempid[\"9fdf784a-aaca-46b1-8b9d-019b513b2c83\"]"}}]}
      {:editor-change/dt 52,
       :db/id "#fulcro/tempid[\"9731d0e0-25c8-4bf7-a90e-a5365b8bf8ff\"]",
       :editor-change/change
       {:editor-text-change/text ["l"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"31a651fd-9d89-4c61-8fe1-52ac7b29da75\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 16,
         :db/id
         "#fulcro/tempid[\"aabc2b90-48f1-477f-903b-522706cc936e\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 16,
         :db/id
         "#fulcro/tempid[\"e0565061-47fe-4525-a394-fc792dbf5411\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"f2f3e47e-b39a-4938-8e47-c6996f94d08f\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"b8b68fa5-7a2d-4256-a02d-2cbb236d9617\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 17,
          :db/id
          "#fulcro/tempid[\"e1c5c17a-b75c-4d9f-b81b-716341f8d92c\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 17,
          :db/id
          "#fulcro/tempid[\"3efd39fb-98f7-4333-96e2-b794c529b484\"]"}}]}
      {:editor-change/dt 93,
       :db/id "#fulcro/tempid[\"7bfb7beb-dcb6-4758-8c64-e1d8f18fd2e1\"]",
       :editor-change/change
       {:editor-text-change/text ["y"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"80c61878-acd4-47cb-82cc-30c30e282455\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 17,
         :db/id
         "#fulcro/tempid[\"f40456e2-b797-4fa4-b5ce-b71a96bfb793\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 17,
         :db/id
         "#fulcro/tempid[\"95fc6064-c1ba-4742-9d5c-de2d925fbe73\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"2c8c0fcd-ae18-45f3-b73f-3af4d129a459\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"5fb33451-9315-4c40-bdf1-175cfb749ed6\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 18,
          :db/id
          "#fulcro/tempid[\"737cd159-aa84-491c-99ff-2aef272da27a\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 18,
          :db/id
          "#fulcro/tempid[\"7269d9c4-43cb-40cb-975b-c8e11b2679a7\"]"}}]}
      {:editor-change/dt 283,
       :db/id "#fulcro/tempid[\"5581d4fb-954f-430d-9a5b-0aa61f63bb1f\"]",
       :editor-change/change
       {:editor-text-change/text ["D"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"8e8ff50a-59fb-482a-b27f-2a1106a27b8f\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 18,
         :db/id
         "#fulcro/tempid[\"c4574868-b241-4aad-b870-4151a0b07574\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 18,
         :db/id
         "#fulcro/tempid[\"bf8aaa66-603b-48be-9f33-838b00d4fa0f\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"129aba4d-ce4c-483c-8b40-f35b5da24da6\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"839d23ff-87b0-4f13-8651-f798fdfbbe54\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 19,
          :db/id
          "#fulcro/tempid[\"6d6e870c-79f4-4b4a-ab03-647fbc8749f0\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 19,
          :db/id
          "#fulcro/tempid[\"80865d32-38a9-4606-95bc-55de354bee83\"]"}}]}
      {:editor-change/dt 161,
       :db/id "#fulcro/tempid[\"771196d9-ccc8-46bb-8540-92d20f32f3da\"]",
       :editor-change/change
       {:editor-text-change/text ["e"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"38cde5b9-99b4-44ea-a17d-aba4b0e14871\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 19,
         :db/id
         "#fulcro/tempid[\"64bbff20-fd06-437b-971b-12bf037bcd29\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 19,
         :db/id
         "#fulcro/tempid[\"511cc388-ffa1-43b8-b6dd-624d975c32bf\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"e61eb1ce-4a2e-4616-a9b9-93d10c988843\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"caa31806-e4c2-47ca-91d9-9dd7ce82c636\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 20,
          :db/id
          "#fulcro/tempid[\"28da474e-2071-4120-aaaa-7657a8287ec5\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 20,
          :db/id
          "#fulcro/tempid[\"b6f20261-c627-45b4-94be-ace67c067fcc\"]"}}]}
      {:editor-change/dt 139,
       :db/id "#fulcro/tempid[\"6722bb93-a5d4-4072-af4a-055fd2a1ee24\"]",
       :editor-change/change
       {:editor-text-change/text ["p"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"d2ac062c-8952-4ab4-97bf-fccce6489e7c\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 20,
         :db/id
         "#fulcro/tempid[\"344ac485-3433-4702-9cc2-28b35676477b\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 20,
         :db/id
         "#fulcro/tempid[\"009b47de-e817-4285-8119-6e1dfab9cd16\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"e0278be9-b3d9-4174-8cc0-6e4585263e80\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"3466fe7e-f570-4d41-a701-e400c3dec5a2\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 21,
          :db/id
          "#fulcro/tempid[\"25eddeb5-1a5a-41ae-91c4-d0e2280c6526\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 21,
          :db/id
          "#fulcro/tempid[\"fe7c5e9e-e7e9-41c4-b33e-8a9b1b3abbe9\"]"}}]}
      {:editor-change/dt 86,
       :db/id "#fulcro/tempid[\"6775f9ed-2206-490c-b299-5ecefffdc901\"]",
       :editor-change/change
       {:editor-text-change/text ["o"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"0dccef28-c7ee-48d8-a627-1117c60d05eb\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 21,
         :db/id
         "#fulcro/tempid[\"860b59bd-6ba3-4729-ae02-42e807aa381b\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 21,
         :db/id
         "#fulcro/tempid[\"795cff8b-a981-45fc-bcd3-e4a844832fde\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"a5171287-83a5-4e78-955c-a344f473b38b\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"f65364cc-674a-4aaf-b9fd-9c5fb19e11b5\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 22,
          :db/id
          "#fulcro/tempid[\"9d8a6bb4-2047-453b-bdd4-a7f3e91141d3\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 22,
          :db/id
          "#fulcro/tempid[\"75f4f2bd-982b-4c7f-b00b-55f672811c25\"]"}}]}
      {:editor-change/dt 96,
       :db/id "#fulcro/tempid[\"ee00fd37-5b9b-488d-b3bb-59b24b4f3b44\"]",
       :editor-change/change
       {:editor-text-change/text ["t"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"c92db355-4150-484c-a60a-40e1a84f88cf\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 22,
         :db/id
         "#fulcro/tempid[\"01815ed3-7a6a-47ee-8ab2-a6aecea8203e\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 22,
         :db/id
         "#fulcro/tempid[\"2bcef8a0-0ba1-42e8-87d7-d72e3e312661\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"6c15ad64-2f23-4d30-988c-c88a1e188050\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"c5aa2ec1-f84e-4a1a-aa1c-e3a6da7908cf\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 23,
          :db/id
          "#fulcro/tempid[\"947bafba-582b-4b83-ae29-f9cf2472b780\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 23,
          :db/id
          "#fulcro/tempid[\"0a055a8c-9313-432d-a2a7-ec0a78913191\"]"}}]}
      {:editor-change/dt 220,
       :db/id "#fulcro/tempid[\"c1170917-c9d7-4e5f-87b3-81afd5e5e12c\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"3b5566d1-926d-49a3-b926-debee2ea6a60\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 24,
          :db/id
          "#fulcro/tempid[\"ac8dfd18-5aa6-482a-80d1-47489853a1a5\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 24,
          :db/id
          "#fulcro/tempid[\"bc61dd2e-4b52-4a03-9882-c7b6b9d0f854\"]"}}]}
      {:editor-change/dt 324,
       :db/id "#fulcro/tempid[\"5438c648-09d5-4f24-80bc-9d4bb1380381\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"c329936f-d9aa-4d2c-aff2-99158e606a99\"]",
         :editor-selection/anchor
         {:editor-caret/line 1,
          :editor-caret/ch 25,
          :db/id
          "#fulcro/tempid[\"eb53dd4e-5ae8-4a90-aea3-0dcc31a715fb\"]"},
         :editor-selection/head
         {:editor-caret/line 1,
          :editor-caret/ch 25,
          :db/id
          "#fulcro/tempid[\"e1b06d76-959a-4a13-bf23-0706a2c2feae\"]"}}]}
      {:editor-change/dt 364,
       :db/id "#fulcro/tempid[\"c6c43f8d-c425-4453-8d4d-68767a8c8195\"]",
       :editor-change/change
       {:editor-text-change/text ["" ""],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"a7320d60-22e9-4a14-bf78-479fd3eac3ac\"]",
        :editor-text-change/from
        {:editor-caret/line 1,
         :editor-caret/ch 25,
         :db/id
         "#fulcro/tempid[\"0bd79def-6ccd-4b12-ad5a-016a082b697a\"]"},
        :editor-text-change/to
        {:editor-caret/line 1,
         :editor-caret/ch 25,
         :db/id
         "#fulcro/tempid[\"49e97123-2809-43c9-8ddb-ebe2344ec603\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"14438605-3694-408d-b17d-4458523b91fd\"]",
       :editor-change/change
       {:editor-text-change/text ["    "],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"a9237bd5-b406-48d8-8f27-6e5e016ea576\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 0,
         :db/id
         "#fulcro/tempid[\"649d81a0-69fe-4a58-bcbb-21bb152340dd\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 0,
         :db/id
         "#fulcro/tempid[\"fd4ec252-18df-4fdf-b5e4-68ddc92b8c82\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"ff7c0f36-83da-44e2-b8b9-8f9d5202ebc3\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"a7251c8c-882a-4be0-9760-ca730d646cda\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"5d878ef7-3aec-4b97-ae86-ac4e20cf7a13\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"3bddb52c-fb68-4ca7-b72d-0b9456c63e0a\"]"}}]}
      {:editor-change/dt 8,
       :db/id "#fulcro/tempid[\"ae97c990-7141-4fa5-ab80-c7a0a860f170\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"b1e4ef96-7b39-4e9f-aee3-3cfd8791b3a0\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"72966715-8b17-4b26-90ec-2008d5b294ad\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"b5228d24-6ab2-48cd-8b5a-c67cf033b4e3\"]"}}]}
      {:editor-change/dt 5,
       :db/id "#fulcro/tempid[\"899720c8-25f3-4b45-a4c7-810fdcfb8f34\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"6943d2f9-2855-4131-8be5-0139a1c550d4\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"67ed7c29-f11d-4c26-80cf-49b4eb0a4025\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"1bd9dbff-6e59-4d31-a36a-f3fd8b7eed2d\"]"}}]}
      {:editor-change/dt 288,
       :db/id "#fulcro/tempid[\"778492b6-a90b-412c-b6e1-85ebd4b6ce5c\"]",
       :editor-change/change
       {:editor-text-change/text ["()"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"d80e7d05-27f8-4329-8bcc-56d145131152\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 4,
         :db/id
         "#fulcro/tempid[\"59ed09ff-54a4-4486-b0a4-d7ea215220a8\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 4,
         :db/id
         "#fulcro/tempid[\"2c37de84-0ad5-411f-9901-a3131cae5c1e\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"852e0704-324e-449d-94b8-9834803ea0d2\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"b278a4da-5851-4d57-8c09-f9fdc86189dd\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 5,
          :db/id
          "#fulcro/tempid[\"45d0d9a3-3d47-4c21-bd92-6dd93c5c2030\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 5,
          :db/id
          "#fulcro/tempid[\"73424876-953d-4ec0-9e5e-8feb5770ef2b\"]"}}]}
      {:editor-change/dt 5,
       :db/id "#fulcro/tempid[\"d71094cf-3895-44ec-92e7-2db0f744e393\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"880a1f36-c766-482a-b9ec-008a1c009e4a\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"465bc264-987a-49fd-b2ed-1482fa6e27e1\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"0d1b8d19-4b4a-4910-ab55-8a3b99d1fc2d\"]"}}]}
      {:editor-change/dt 4,
       :db/id "#fulcro/tempid[\"998a4274-3fad-40cb-a3f8-6307f40472f2\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"ea5273f1-e083-4076-b2bd-68b04fefe668\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 5,
          :db/id
          "#fulcro/tempid[\"444003be-06af-46c8-bc3d-5c4d7d0e5fb2\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 5,
          :db/id
          "#fulcro/tempid[\"220c9f8b-5ba2-4c97-a947-d4851f04b6e3\"]"}}]}
      {:editor-change/dt 262,
       :db/id "#fulcro/tempid[\"64db3179-4dfd-4a38-8d59-881ded0f6f61\"]",
       :editor-change/change
       {:editor-text-change/text ["b"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"b9fd6d45-42a9-42c9-81b2-1887e646cdbb\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 5,
         :db/id
         "#fulcro/tempid[\"dcc6ac22-2d37-4403-b090-d5ac52580647\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 5,
         :db/id
         "#fulcro/tempid[\"6c634b95-3bdd-426f-ab8c-811648d961e8\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"3f434209-d855-4432-a42b-0045e8146665\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"ddb6d0c9-11de-4c3b-aace-9a3c037f8459\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 6,
          :db/id
          "#fulcro/tempid[\"02881245-458d-4d46-816a-bd5e0a9722b0\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 6,
          :db/id
          "#fulcro/tempid[\"49c94586-4c3d-40bb-a12e-ee9405cfde24\"]"}}]}
      {:editor-change/dt 67,
       :db/id "#fulcro/tempid[\"70de5b96-0614-4b25-a666-1c2037b76db3\"]",
       :editor-change/change
       {:editor-text-change/text ["u"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"f2faebe9-e90c-4eb9-9082-8d30c1f6f551\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 6,
         :db/id
         "#fulcro/tempid[\"dab1195e-fe4d-442d-8831-253b4a44dc92\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 6,
         :db/id
         "#fulcro/tempid[\"4c4b5e02-70fe-4b87-b621-a5abc51d8ba1\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"03816bfa-7efd-4db9-9c6e-af0f8d601735\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"7a25e4bb-3cc1-4cff-aaf3-8e8067b78b61\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 7,
          :db/id
          "#fulcro/tempid[\"2aa88279-07f4-4477-9594-907df10d3374\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 7,
          :db/id
          "#fulcro/tempid[\"eb231cb1-c211-485f-8e05-3596bdab86d2\"]"}}]}
      {:editor-change/dt 86,
       :db/id "#fulcro/tempid[\"7f8fbd2d-127e-4ef2-b0ca-1ef6555bc0da\"]",
       :editor-change/change
       {:editor-text-change/text ["i"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"337c2d9d-e10d-45f3-b67e-9f79a0410a4f\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 7,
         :db/id
         "#fulcro/tempid[\"0b06c03b-1efd-4dd4-8c17-25338bb6c884\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 7,
         :db/id
         "#fulcro/tempid[\"307c185a-7db8-4c67-9a81-2c2543b7dcfa\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"58eb2777-30fd-4bd1-8a52-c13616d8015e\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"c4b764bf-993a-4ab8-9ee8-949a6034d367\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 8,
          :db/id
          "#fulcro/tempid[\"d0fed24c-1166-4e77-b299-c3754bf35381\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 8,
          :db/id
          "#fulcro/tempid[\"c54b6db1-e36e-4aaf-bb60-ab66f876dc2b\"]"}}]}
      {:editor-change/dt 102,
       :db/id "#fulcro/tempid[\"f903e479-2331-4106-8e0f-1e69955684cc\"]",
       :editor-change/change
       {:editor-text-change/text ["l"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"e30a90fb-6af6-4e4d-aecd-064546dccf11\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 8,
         :db/id
         "#fulcro/tempid[\"a4681860-8c5a-48e2-b41b-6e0f1fd3ed90\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 8,
         :db/id
         "#fulcro/tempid[\"1c7b26ad-a429-4ef3-8b74-0611486efb3c\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"3f0a1748-4489-4ee7-a01e-926670753227\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"16688139-02d0-49c9-afad-00192a6be6f3\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 9,
          :db/id
          "#fulcro/tempid[\"fcb55c18-bc7f-4618-8000-5615e13ec86d\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 9,
          :db/id
          "#fulcro/tempid[\"15a6ad59-fc11-40dd-941a-c557404fff2f\"]"}}]}
      {:editor-change/dt 87,
       :db/id "#fulcro/tempid[\"665bb4b3-e188-4298-9602-34f0634de5e6\"]",
       :editor-change/change
       {:editor-text-change/text ["d"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"51a999cf-4d1b-4580-9800-6fe4f913e53e\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 9,
         :db/id
         "#fulcro/tempid[\"4fc678b4-9190-420a-88d3-c98266a38a29\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 9,
         :db/id
         "#fulcro/tempid[\"cb8e0770-7b33-46e5-b6e3-b382903f5916\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"6d8ec945-b458-4d0d-b4f6-2572b6796020\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"58eda455-34ed-452b-8fa0-24ef1dc65030\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 10,
          :db/id
          "#fulcro/tempid[\"4bf9ee01-5298-4bcd-820c-47635d091d74\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 10,
          :db/id
          "#fulcro/tempid[\"fd020b76-e94c-4951-bde7-5a0f111d4be3\"]"}}]}
      {:editor-change/dt 134,
       :db/id "#fulcro/tempid[\"764a5530-8f1f-4945-a056-f07f0a8027c0\"]",
       :editor-change/change
       {:editor-text-change/text [" "],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"308e8477-3598-441a-82d0-d44beccd299a\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 10,
         :db/id
         "#fulcro/tempid[\"43692400-6d69-4597-9256-be4766bba2c0\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 10,
         :db/id
         "#fulcro/tempid[\"f03b361c-054c-474b-bd22-50c0177e1e73\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"6c0c1ee6-d7ba-4a0c-926d-92935257b2c0\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"abcde318-f234-4c4e-af9f-984db30cb1da\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 11,
          :db/id
          "#fulcro/tempid[\"1df3693e-8a83-4fd5-9cad-db243528bce9\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 11,
          :db/id
          "#fulcro/tempid[\"f482f3ef-41de-4dff-95b2-b2b1848cc374\"]"}}]}
      {:editor-change/dt 179,
       :db/id "#fulcro/tempid[\"2ab1f05b-f421-4787-b368-78a06b05b61f\"]",
       :editor-change/change
       {:editor-text-change/text ["\"\""],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"b1d62b88-8391-4bf4-930f-02c4dd60d6ca\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 11,
         :db/id
         "#fulcro/tempid[\"e5ceaa37-cc93-4d43-b2bd-f90e345cf407\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 11,
         :db/id
         "#fulcro/tempid[\"ea326310-850f-4ac2-acf6-187c37f0585d\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"5b125eb2-2308-4707-ab3e-54237873f137\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"e65bd399-182a-48fa-869e-45ce63fa5cc6\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 12,
          :db/id
          "#fulcro/tempid[\"3fab04dd-565b-48b0-a45f-93a0edf62cfd\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 12,
          :db/id
          "#fulcro/tempid[\"595e20f6-a92b-438b-b055-ff4314382b6c\"]"}}]}
      {:editor-change/dt 786,
       :db/id "#fulcro/tempid[\"475cadc6-b81f-4499-ae0b-eb544d9cda87\"]",
       :editor-change/change
       {:editor-text-change/text ["B"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"d614a213-7328-42b0-af2f-9e5a4373d394\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 12,
         :db/id
         "#fulcro/tempid[\"70910df5-ab90-4a68-8b3b-1f5ec35e6ae9\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 12,
         :db/id
         "#fulcro/tempid[\"33b6cc77-1816-4db5-babf-a4243bd0c373\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"0dd541e4-713f-4bb9-bea9-3e793b868959\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"db7282cc-c94c-46ea-9f44-6ccc81fe1962\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 13,
          :db/id
          "#fulcro/tempid[\"00999634-0671-443d-8f11-32b243fa64d9\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 13,
          :db/id
          "#fulcro/tempid[\"f881acf9-e8d0-46ac-a26f-4f853febba6f\"]"}}]}
      {:editor-change/dt 211,
       :db/id "#fulcro/tempid[\"c3dea4d3-1e16-4b8c-8cb9-c747a7b53823\"]",
       :editor-change/change
       {:editor-text-change/text ["a"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"d0f65fd7-5105-4c13-9b4f-4e7ce968c080\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 13,
         :db/id
         "#fulcro/tempid[\"39acb20c-db3f-4a17-a239-f0dabf21c934\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 13,
         :db/id
         "#fulcro/tempid[\"fe65d10f-275c-4395-bd47-ce141990306c\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"12c693c1-74d3-4b2a-8eb4-74b5a30bf9ad\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"15a2df55-5dea-46a9-8d2a-d6912f98a659\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 14,
          :db/id
          "#fulcro/tempid[\"e54e2c66-b587-4a66-a973-6f0388f99bf1\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 14,
          :db/id
          "#fulcro/tempid[\"528709da-99da-403d-8408-582045412446\"]"}}]}
      {:editor-change/dt 57,
       :db/id "#fulcro/tempid[\"2cea6271-73bd-4393-9698-3333ea01ec4b\"]",
       :editor-change/change
       {:editor-text-change/text ["r"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"97948f88-2618-4fc5-9a83-a27f4ef8494b\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 14,
         :db/id
         "#fulcro/tempid[\"3b11b657-3259-47e6-bafb-0a54c68c29e9\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 14,
         :db/id
         "#fulcro/tempid[\"8f2a9cef-966b-44cd-9d51-88465cb8ab66\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"36b43304-3494-4cd7-b533-bd11c718e00d\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"1229ff02-759e-4216-9cdf-23d61f5ee580\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 15,
          :db/id
          "#fulcro/tempid[\"ff9c27f6-424f-447a-b9ff-a30aeb240c48\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 15,
          :db/id
          "#fulcro/tempid[\"dba6b397-3f85-41e0-b73e-0b358bb13373\"]"}}]}
      {:editor-change/dt 197,
       :db/id "#fulcro/tempid[\"9fdb9538-726d-4cbd-8c86-453fc31a712d\"]",
       :editor-change/change
       {:editor-text-change/text ["r"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"fb08dead-4c74-4a33-9e1a-3273981c8855\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 15,
         :db/id
         "#fulcro/tempid[\"ee167c7b-a9b0-4f1d-bd7a-d805d7eff86a\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 15,
         :db/id
         "#fulcro/tempid[\"4e2f6aa0-d972-4dd7-b1d2-dba4c62881cc\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"660fd501-62d6-4615-8da5-336f6846adcd\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"1edf2370-f89a-4d85-9a2c-783b05be1bb7\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 16,
          :db/id
          "#fulcro/tempid[\"1f686fe5-79e2-4944-ac6b-ea8f8d3b1073\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 16,
          :db/id
          "#fulcro/tempid[\"6937be6b-05a1-4be8-bc33-2a1e6357b956\"]"}}]}
      {:editor-change/dt 166,
       :db/id "#fulcro/tempid[\"6b1cf452-c65e-4718-b3ca-3583fd31e6f2\"]",
       :editor-change/change
       {:editor-text-change/text ["a"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"dff7e001-6197-42fc-b1b2-4de4e62bbcca\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 16,
         :db/id
         "#fulcro/tempid[\"e541d80f-6589-4071-8294-473570f12318\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 16,
         :db/id
         "#fulcro/tempid[\"5f960c83-1408-45d5-b4c5-16619dfa3445\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"7238d0c3-b477-4322-beab-3a2e2ec69e2d\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"362e6556-e16e-42e9-b78b-ccd3b6df4ab5\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 17,
          :db/id
          "#fulcro/tempid[\"10b4bb87-e9dd-48bf-a0bc-0535b8538177\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 17,
          :db/id
          "#fulcro/tempid[\"f6156adc-3657-4a9d-929d-a57548287270\"]"}}]}
      {:editor-change/dt 155,
       :db/id "#fulcro/tempid[\"df6192c7-5e66-43d8-84a3-4e247db3131a\"]",
       :editor-change/change
       {:editor-text-change/text ["c"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"1410b0f7-4f1f-46f9-8849-f9da064a6cd8\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 17,
         :db/id
         "#fulcro/tempid[\"ccf68561-3c05-491f-9596-75001102755d\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 17,
         :db/id
         "#fulcro/tempid[\"5578c8b6-b50b-489b-8a7c-93646385575e\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"3198f444-041d-4be8-8bf0-5ae11c35b656\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"55e80f84-0ec2-4829-a9c8-ceb00cfb04fe\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 18,
          :db/id
          "#fulcro/tempid[\"e887e9f4-ebdc-435d-bc47-1529577162d3\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 18,
          :db/id
          "#fulcro/tempid[\"c79577a2-8cbb-4429-8ab9-b686a654af1d\"]"}}]}
      {:editor-change/dt 118,
       :db/id "#fulcro/tempid[\"abc644a4-3fd8-47dc-90f8-efcfd95b8484\"]",
       :editor-change/change
       {:editor-text-change/text ["k"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"2c3baaf6-076e-4ffc-95d7-d3847bdcbbfc\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 18,
         :db/id
         "#fulcro/tempid[\"0b49f814-45f2-460e-b905-8f34154ab1e4\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 18,
         :db/id
         "#fulcro/tempid[\"b61e9a3e-0850-41c9-805b-1b08db1d61dd\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"10e971cf-3702-4ce1-8626-bf2acd722fab\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"e0b359a2-74a9-44da-9e72-b1ca3a79eb11\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 19,
          :db/id
          "#fulcro/tempid[\"818279f9-5122-4486-84ac-4c75f1e1dd2a\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 19,
          :db/id
          "#fulcro/tempid[\"1ba73aa4-ef67-48b2-9558-0058adb92418\"]"}}]}
      {:editor-change/dt 99,
       :db/id "#fulcro/tempid[\"f79cd945-a8dc-4045-aecf-ae18cb0708a0\"]",
       :editor-change/change
       {:editor-text-change/text ["s"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"cbb0c6da-fa17-4fe8-a96f-5c05da2fc467\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 19,
         :db/id
         "#fulcro/tempid[\"332169e9-8062-4b63-b46b-bc2d6e70946b\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 19,
         :db/id
         "#fulcro/tempid[\"1d7e6e37-a339-4907-950e-fce4c5e5f773\"]"}}}
      {:editor-change/dt 2,
       :db/id "#fulcro/tempid[\"0d3cec09-1218-484c-bdf0-f888f5d749f5\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"7dccdb7c-5e82-4a2f-98e8-d9d0fcb811d1\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 20,
          :db/id
          "#fulcro/tempid[\"9ddc9952-0c2a-4301-a626-0f1ab657fb67\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 20,
          :db/id
          "#fulcro/tempid[\"53443e4b-98d2-4447-aaad-2566a571728e\"]"}}]}
      {:editor-change/dt 247,
       :db/id "#fulcro/tempid[\"2339b1ec-ee26-4437-ad65-56ffb44fb12e\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"5b236e85-938a-42e0-874b-9652edcc811f\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 21,
          :db/id
          "#fulcro/tempid[\"7e805a90-74b2-4069-aef7-d121b45e9c3b\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 21,
          :db/id
          "#fulcro/tempid[\"bf2d2823-5771-42e7-aff1-bf528b14af90\"]"}}]}
      {:editor-change/dt 368,
       :db/id "#fulcro/tempid[\"ee840764-4708-4e40-ad66-4c477b65c877\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"0ab533ac-dc82-4bf8-90c5-64e32b5a2759\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 22,
          :db/id
          "#fulcro/tempid[\"6f401a3b-e2e2-41a2-b143-38b71059a340\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 22,
          :db/id
          "#fulcro/tempid[\"fa7d6435-8de3-46bc-bb17-4a61673182ef\"]"}}]}
      {:editor-change/dt 1210,
       :db/id "#fulcro/tempid[\"946dcbdd-e501-4065-aa4d-a47ee8384e5e\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"d330a42a-799e-4de2-a127-09af0bb1e789\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 21,
          :db/id
          "#fulcro/tempid[\"36247757-35a5-43bf-8be9-2bb50409f0cb\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 21,
          :db/id
          "#fulcro/tempid[\"eb3e9c05-60ce-47ab-ab0f-89da36b2dc12\"]"}}]}
      {:editor-change/dt 233,
       :db/id "#fulcro/tempid[\"8ff79c95-acbd-4b75-931b-7987342bbbc8\"]",
       :editor-change/change
       {:editor-text-change/text [" "],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"1828b231-fadc-43e8-96b0-211f942300fb\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 21,
         :db/id
         "#fulcro/tempid[\"168a031c-d2bc-4b7a-abe6-b1e889ba4e27\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 21,
         :db/id
         "#fulcro/tempid[\"0899be1c-5825-4584-b8e7-a60c0471f464\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"35126b13-e8e7-4a45-8430-0f10cfc1e774\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"e2a8565b-d8ed-4913-ad09-fc15d17fda7f\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 22,
          :db/id
          "#fulcro/tempid[\"fa8b7831-9bab-4577-8de6-f132632a8454\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 22,
          :db/id
          "#fulcro/tempid[\"52d79c90-b084-4430-8b07-30e4a7419e42\"]"}}]}
      {:editor-change/dt 747,
       :db/id "#fulcro/tempid[\"0c0f2ffb-d9fa-4ded-a225-c7da250ae2f8\"]",
       :editor-change/change
       {:editor-text-change/text [":"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"af7ca8f0-6227-4407-b28f-46dd064396bc\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 22,
         :db/id
         "#fulcro/tempid[\"8a4618e2-83ff-494d-bad2-1ab3b30f1a6e\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 22,
         :db/id
         "#fulcro/tempid[\"11ce00f9-a1ea-4b8d-bcfa-cd8e4da00341\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"628b70b2-6d8c-47b4-9ca7-8f26df8a0ad9\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"4b65f4ff-6371-44f8-b466-2cf080b877c0\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 23,
          :db/id
          "#fulcro/tempid[\"13a55c2a-a4af-4f67-8bb5-9e97ce93f6b0\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 23,
          :db/id
          "#fulcro/tempid[\"a56e9533-29e1-44fd-acd2-bb6ca5f204df\"]"}}]}
      {:editor-change/dt 298,
       :db/id "#fulcro/tempid[\"64ef3c57-77f2-4db3-be54-c088390996ff\"]",
       :editor-change/change
       {:editor-text-change/text ["u"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"39793564-b8d7-44f6-be69-f95bd5741229\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 23,
         :db/id
         "#fulcro/tempid[\"7c6cec23-894c-45e7-85f2-2a850cc673dd\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 23,
         :db/id
         "#fulcro/tempid[\"3b378333-c158-47c9-a8b9-7d673f4ac0e3\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"7b62d902-44db-40fc-b929-844ea52de4d5\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"0f1fb36c-ea7b-44ee-a237-79a0e6d252f0\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 24,
          :db/id
          "#fulcro/tempid[\"3f80c5bd-67b0-40c8-9a1c-421839263f23\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 24,
          :db/id
          "#fulcro/tempid[\"df05b3f6-f5a5-47c1-9411-1d031b8d9974\"]"}}]}
      {:editor-change/dt 94,
       :db/id "#fulcro/tempid[\"5877241f-86d1-4e27-b4fe-4bfed28b59b2\"]",
       :editor-change/change
       {:editor-text-change/text ["n"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"43d40237-0b15-4fbe-8056-b282843dd747\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 24,
         :db/id
         "#fulcro/tempid[\"28bfe401-6909-4253-ab48-7637d706d4c7\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 24,
         :db/id
         "#fulcro/tempid[\"d8d91579-a37c-4eb1-95c0-c08770f58699\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"a736fac3-a43c-40b2-bf59-577538f82a3d\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"a6cdf12d-38f8-4877-b239-80a711d73721\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 25,
          :db/id
          "#fulcro/tempid[\"f4161ea7-d37b-4c15-8b72-7d996ff7ea9f\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 25,
          :db/id
          "#fulcro/tempid[\"6f933173-5268-4f69-9141-e6ea9d640890\"]"}}]}
      {:editor-change/dt 155,
       :db/id "#fulcro/tempid[\"f810a6cc-4192-48a1-8d1f-76462ce050fd\"]",
       :editor-change/change
       {:editor-text-change/text ["t"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"f47a8059-7b34-46d2-9db8-4850b9fd3a34\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 25,
         :db/id
         "#fulcro/tempid[\"abba9637-ab0f-473e-8b67-c36fb680fb0a\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 25,
         :db/id
         "#fulcro/tempid[\"33721ce1-fa18-4081-b11f-dabd171e4ab7\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"705066f6-a9ef-47b3-9983-005c6fe7c412\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"320f7ff8-58c5-4109-9c61-a1cd377b040f\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 26,
          :db/id
          "#fulcro/tempid[\"98d4df49-39b9-4543-a4d1-864a83db51e3\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 26,
          :db/id
          "#fulcro/tempid[\"51df88ff-86f2-4d68-87eb-e8736fb7da59\"]"}}]}
      {:editor-change/dt 78,
       :db/id "#fulcro/tempid[\"ceaa0118-7355-4b58-bcd0-939713394d2f\"]",
       :editor-change/change
       {:editor-text-change/text ["i"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"9fe628c3-4c1f-4eb6-bd20-080052ace1a4\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 26,
         :db/id
         "#fulcro/tempid[\"80727d05-6906-4740-bb70-07c804e766b6\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 26,
         :db/id
         "#fulcro/tempid[\"040eb037-b84e-4ec6-b146-9abdbf98f4d7\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"d7c14d6b-8cc4-42d2-87bf-437adab5c52d\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"923a37f1-9499-41f7-8f4c-5adde4a910bf\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 27,
          :db/id
          "#fulcro/tempid[\"38cf602a-c449-4e7e-a8e8-ee12bdcca0c1\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 27,
          :db/id
          "#fulcro/tempid[\"2260b03c-9497-4e8f-94f0-d5208fe80b55\"]"}}]}
      {:editor-change/dt 622,
       :db/id "#fulcro/tempid[\"7f04207c-0db1-47df-b7a6-45650d939932\"]",
       :editor-change/change
       {:editor-text-change/text ["l"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"47fcee02-f58f-49f7-a593-13000d2c2bb0\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 27,
         :db/id
         "#fulcro/tempid[\"8be48145-ac40-469f-9e76-b6b9ed6f5a65\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 27,
         :db/id
         "#fulcro/tempid[\"cae63fc6-aad0-4343-9499-eca674b2dc0b\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"5c502f55-083a-43ae-98af-81860ff8bfc4\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"c4bb89b6-c24b-4d47-abd2-bc66b57e5c36\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 28,
          :db/id
          "#fulcro/tempid[\"4e063e69-0c12-4e13-a833-3739955c5785\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 28,
          :db/id
          "#fulcro/tempid[\"b5396778-6362-41e2-b56d-a2fd72e195b1\"]"}}]}
      {:editor-change/dt 188,
       :db/id "#fulcro/tempid[\"48e4b8d1-182a-4340-a6aa-f1065841d8f4\"]",
       :editor-change/change
       {:editor-text-change/text ["-"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"d4222949-3b86-4abe-a1d3-53b3e434960f\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 28,
         :db/id
         "#fulcro/tempid[\"4092355b-2a95-4955-b163-26291208960c\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 28,
         :db/id
         "#fulcro/tempid[\"4c324b7e-2989-484a-8020-8fc579a02952\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"3031c87f-bd20-49f7-add0-4bd6c24cab35\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"5dcf0332-9b55-43d9-8dad-5e5b5106dd80\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 29,
          :db/id
          "#fulcro/tempid[\"ed2f85dd-c29f-430b-91d0-ef971ebce9ef\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 29,
          :db/id
          "#fulcro/tempid[\"abd3ad48-ab34-4000-b2c6-c800c34fe01b\"]"}}]}
      {:editor-change/dt 128,
       :db/id "#fulcro/tempid[\"2ca18997-8166-4f87-a879-5a5a986a34d2\"]",
       :editor-change/change
       {:editor-text-change/text ["c"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"be24af5a-6419-4100-aa45-4ef162479441\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 29,
         :db/id
         "#fulcro/tempid[\"8758c7fc-ebbd-4850-9223-a77243fd4c21\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 29,
         :db/id
         "#fulcro/tempid[\"fc13d13b-64e9-4a66-aa17-a09459affd63\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"5b4088ee-0c04-4cb9-aba4-e880f6792ce1\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"e9f47690-66c0-47a6-b3f9-b0cb44c91d5a\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 30,
          :db/id
          "#fulcro/tempid[\"d5c8cdb0-f558-474f-908b-49aa6e3d360d\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 30,
          :db/id
          "#fulcro/tempid[\"4cfa73a0-d483-4d02-ac20-f5bb4a6f4b8f\"]"}}]}
      {:editor-change/dt 79,
       :db/id "#fulcro/tempid[\"6b93d817-d11a-4478-9529-1e2315522d60\"]",
       :editor-change/change
       {:editor-text-change/text ["o"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"15bf3821-cea4-4131-a7d8-684637ad2aaa\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 30,
         :db/id
         "#fulcro/tempid[\"3fcfb468-4ea0-4a63-9e31-b25b4f35d755\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 30,
         :db/id
         "#fulcro/tempid[\"bd8b2f68-e844-4ff1-ae98-54e055ac2c32\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"72a70332-5680-4b18-b3b7-8f1f29f147d7\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"c8ebe2b9-c092-4b86-a9f8-2383ba330194\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 31,
          :db/id
          "#fulcro/tempid[\"3354fbc8-665d-4f19-b1c0-1471d1fe8359\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 31,
          :db/id
          "#fulcro/tempid[\"edef8a50-536d-4e07-a2a5-7e023d56dbfb\"]"}}]}
      {:editor-change/dt 91,
       :db/id "#fulcro/tempid[\"a66de77a-4b20-4b6e-842f-4ea84c5cf2db\"]",
       :editor-change/change
       {:editor-text-change/text ["u"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"66179e4a-cc7d-448b-a17e-69eda714efea\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 31,
         :db/id
         "#fulcro/tempid[\"a50e2204-f273-4de8-89e1-0a1d95f3056d\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 31,
         :db/id
         "#fulcro/tempid[\"c5256227-dcd2-4a9f-aff0-c53b9bbc5f27\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"4dfbcbcc-ce2b-437e-b9fa-36f567843804\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"89fccf98-25dc-48cc-bfff-0978653c1784\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 32,
          :db/id
          "#fulcro/tempid[\"718c7164-14a8-4d6d-a266-495108844e8a\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 32,
          :db/id
          "#fulcro/tempid[\"93312546-8988-4dbe-9384-2be9c4f5e095\"]"}}]}
      {:editor-change/dt 70,
       :db/id "#fulcro/tempid[\"5b145572-b8cb-4026-a41e-133559fec08f\"]",
       :editor-change/change
       {:editor-text-change/text ["n"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"7a4a8b2a-b53f-46de-a2bd-6475785bf19c\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 32,
         :db/id
         "#fulcro/tempid[\"4ecc28b3-0d6c-4ff2-83ea-d72bdb3f9eb0\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 32,
         :db/id
         "#fulcro/tempid[\"3784d8c4-fbec-4c1d-a1f2-65b63daa2b00\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"f3f26cd2-dd81-4836-94e7-83a0343f37ef\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"a46cae58-5095-44ca-8cc3-3b3bee374632\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 33,
          :db/id
          "#fulcro/tempid[\"8b7afaf7-3fb6-4487-b116-12f48f2699ee\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 33,
          :db/id
          "#fulcro/tempid[\"a3152c18-0020-439b-9006-ccc3d44368da\"]"}}]}
      {:editor-change/dt 95,
       :db/id "#fulcro/tempid[\"13d8d59b-b303-4341-bc21-231ca1240a7b\"]",
       :editor-change/change
       {:editor-text-change/text ["t"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"2f693b0c-e824-4200-84f1-3b814992a975\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 33,
         :db/id
         "#fulcro/tempid[\"690dcc5c-193e-4afa-a321-b18dd543fbbc\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 33,
         :db/id
         "#fulcro/tempid[\"1b9c3f30-459f-4d56-b738-5cb3b3007620\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"ec81e79e-06d9-4f07-923e-9a911f771a8d\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"ca5531e6-b580-4d37-8189-84d216ccc0b7\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 34,
          :db/id
          "#fulcro/tempid[\"371d4149-896c-4c1f-aa0b-d908f0ef39cd\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 34,
          :db/id
          "#fulcro/tempid[\"b5c2d878-dbf0-4f6c-8708-bbe093f9de42\"]"}}]}
      {:editor-change/dt 384,
       :db/id "#fulcro/tempid[\"cb86b758-fb58-49d9-a4f0-b8c221bfe80a\"]",
       :editor-change/change
       {:editor-text-change/text [" "],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"01849040-8754-4716-939c-b7cbc04f78f8\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 34,
         :db/id
         "#fulcro/tempid[\"9338aacd-2e28-4eef-b5c3-a2b9ad7bd652\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 34,
         :db/id
         "#fulcro/tempid[\"8e9a35e1-eef6-44e7-a887-5dea2f2e31be\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"7d53c098-1dff-4f5e-b253-bcc09cdf498e\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"559a2565-8425-43c9-a3a2-2706706387e4\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 35,
          :db/id
          "#fulcro/tempid[\"7d56071e-4269-4316-b58b-31c7883b5514\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 35,
          :db/id
          "#fulcro/tempid[\"a26c35b2-9bea-49bd-b0d8-491ba936b365\"]"}}]}
      {:editor-change/dt 311,
       :db/id "#fulcro/tempid[\"0cdf156b-e35c-4434-a50d-6a7b1591be5c\"]",
       :editor-change/change
       {:editor-text-change/text ["5"],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"8a552c8e-9631-4655-8064-bd2115dccd55\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 35,
         :db/id
         "#fulcro/tempid[\"7a00cf7a-3d37-49d3-9ad0-be83dc015a88\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 35,
         :db/id
         "#fulcro/tempid[\"b0a41119-7786-4702-80c2-3fbf38e9731a\"]"}}}
      {:editor-change/dt 0,
       :db/id "#fulcro/tempid[\"2d975ae7-9738-40a7-8abc-43252b70a174\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"25674ca4-84da-44b7-ada9-bd0f953b461f\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 36,
          :db/id
          "#fulcro/tempid[\"1d282ecb-7d3e-4cf9-9c6b-f6b48b4e144a\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 36,
          :db/id
          "#fulcro/tempid[\"596da1e7-0260-432d-abcc-aad6e27235d2\"]"}}]}
      {:editor-change/dt 425,
       :db/id "#fulcro/tempid[\"1f922481-b73d-4f80-b02c-6fbafd463723\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"48cffe42-0767-4cda-9b60-6dcb4f9200cb\"]",
         :editor-selection/anchor
         {:editor-caret/line 2,
          :editor-caret/ch 37,
          :db/id
          "#fulcro/tempid[\"caab7732-7765-409e-b6c1-8032e8909780\"]"},
         :editor-selection/head
         {:editor-caret/line 2,
          :editor-caret/ch 37,
          :db/id
          "#fulcro/tempid[\"66a2815d-64e7-43f9-90f9-a21495ab72a2\"]"}}]}
      {:editor-change/dt 375,
       :db/id "#fulcro/tempid[\"457fc3eb-f941-4d3b-8326-0a7ad5475866\"]",
       :editor-change/change
       {:editor-text-change/text ["" ""],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"e35fd5e1-adcd-4db1-82f7-2f2f79b84207\"]",
        :editor-text-change/from
        {:editor-caret/line 2,
         :editor-caret/ch 37,
         :db/id
         "#fulcro/tempid[\"e77929c9-1b95-4f5e-9907-11bd683980ec\"]"},
        :editor-text-change/to
        {:editor-caret/line 2,
         :editor-caret/ch 37,
         :db/id
         "#fulcro/tempid[\"742096df-9594-402f-8f1c-fbf6eed855af\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"a660bfea-46c7-411f-b012-bc34d13a02a1\"]",
       :editor-change/change
       {:editor-text-change/text ["    "],
        :editor-text-change/origin "+input",
        :db/id "#fulcro/tempid[\"3f9b1593-8d5d-4429-932c-0843c05cb63e\"]",
        :editor-text-change/from
        {:editor-caret/line 3,
         :editor-caret/ch 0,
         :db/id
         "#fulcro/tempid[\"2f893ef8-e8b9-4183-a875-52bcb547bed4\"]"},
        :editor-text-change/to
        {:editor-caret/line 3,
         :editor-caret/ch 0,
         :db/id
         "#fulcro/tempid[\"e127710d-1f1f-470a-b73a-afb2f0963125\"]"}}}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"06e06c9e-c299-4757-8fb8-b56293da2711\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"a197d02a-0616-419e-9f17-c05ac9b0ee1f\"]",
         :editor-selection/anchor
         {:editor-caret/line 3,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"742438a2-95b6-453a-99a4-531b6d93373e\"]"},
         :editor-selection/head
         {:editor-caret/line 3,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"d3b49155-569f-4738-a985-647f324b5130\"]"}}]}
      {:editor-change/dt 5,
       :db/id "#fulcro/tempid[\"8e248f40-a4c1-485b-a56a-c097f77e17b3\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"1baea4ce-ad51-434e-aad9-cddc4a0a6cd6\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"cff74e50-549a-47a1-a795-c86c49ac761e\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"1cd31379-850c-4862-abcc-625d5ab83629\"]"}}]}
      {:editor-change/dt 4,
       :db/id "#fulcro/tempid[\"618e127c-425f-428f-a048-ed1af36cc925\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"e7355788-4698-43ed-b7fe-65a0e1055dd4\"]",
         :editor-selection/anchor
         {:editor-caret/line 3,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"55d1674f-80c3-4eff-9335-72e2498de7ce\"]"},
         :editor-selection/head
         {:editor-caret/line 3,
          :editor-caret/ch 4,
          :db/id
          "#fulcro/tempid[\"4cf9980e-035c-42c6-b0f6-351b30d4ccd4\"]"}}]}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"5ee2da0c-6560-4a49-b1c7-7a61d886a3bd\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"0d7c2648-b85d-4a60-8560-63ca42e0efc2\"]",
         :editor-selection/anchor
         {:editor-caret/line 3,
          :editor-caret/ch 9,
          :db/id
          "#fulcro/tempid[\"a51b12a3-8cea-4ac3-810a-4f8011c8fd3e\"]"},
         :editor-selection/head
         {:editor-caret/line 3,
          :editor-caret/ch 9,
          :db/id
          "#fulcro/tempid[\"e3c2d12f-9807-41a4-b8b9-e2ec29b88d05\"]"}}]}
      {:editor-change/dt 1,
       :db/id "#fulcro/tempid[\"66b1d204-ca09-484c-80da-60c103558cc2\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"9730cf9e-7252-4bd9-bede-53d172e39c7a\"]",
         :editor-selection/anchor
         {:editor-caret/line 3,
          :editor-caret/ch 17,
          :db/id
          "#fulcro/tempid[\"c4b12ef3-94ed-4b3e-879e-f1620b96ddf7\"]"},
         :editor-selection/head
         {:editor-caret/line 3,
          :editor-caret/ch 17,
          :db/id
          "#fulcro/tempid[\"9392fcd5-188e-45dd-bde1-38a0ccc50d40\"]"}}]}
      {:editor-change/dt 1930,
       :db/id "#fulcro/tempid[\"dc8db48b-28f4-49ae-aaa7-32c57aad0877\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"52da6312-f69b-4ee8-b8bf-20823da27517\"]",
         :editor-selection/anchor
         {:editor-caret/line 3,
          :editor-caret/ch 18,
          :db/id
          "#fulcro/tempid[\"26321f26-ae9c-4a92-977e-17b1e476525b\"]"},
         :editor-selection/head
         {:editor-caret/line 3,
          :editor-caret/ch 18,
          :db/id
          "#fulcro/tempid[\"42b00dc6-04b5-4208-9629-15ccb7590710\"]"}}]}
      {:editor-change/dt 1205,
       :db/id "#fulcro/tempid[\"65ef6437-648d-4d09-b8a3-afe30cda0da6\"]",
       :editor-change/selections
       [{:db/id "#fulcro/tempid[\"7b5adc1a-a4e2-4b43-9c63-dcac101b1516\"]",
         :editor-selection/anchor
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"54d1b7d4-b704-4614-bcaf-8d92e3ca4914\"]"},
         :editor-selection/head
         {:editor-caret/line 0,
          :editor-caret/ch 0,
          :db/id
          "#fulcro/tempid[\"8dab7ef4-a58a-4862-802b-60cc67894217\"]"}}]}]}
    {:step/description "On a bit lower-level we actually need to deal with the in-game data.
For example when implementing the higher-level \"build\" planner we need to know which units
can build the requested unit type.
We know an SCV can build a SupplyDepot, but how would we get this from the game data?",
     :step/title "Get data from environment",
     :step/init-value "",
     :step/last-time 1532091639111,
     :step/wrap-dom-node :code-cell,
     :db/id "#fulcro/tempid[\"24a78eed-fd4e-461a-9d92-37fb5a24cd0f2\"]",
     :step/changes
     []
     :step/explanation "There is a lot happening here in very little code so let's explain:
cljsc2 when running the game creates a \"knowledge\" object that contains a lot of facts about the game.
We can then use a powerful query engine (datascripts ds/q function) to answer questions about the game.
In this case we find out:
- which ability can build the supply-depot
- what unit-type can use that ability
- all the units in the game of that unit-type
As a result we are interested in the name of the unit-type and the unit-ids
which we can use to send orders to in the game.
"}
    {:db/id 12
     :step/description "Try modifying the previous pieces of code and running them. The best way to learn is to do."
     :step/title "Run your own experiments"}
    ]})
